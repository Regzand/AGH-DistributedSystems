#include <pthread.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <signal.h>
#include <time.h>
#include <stdlib.h>
#include <sys/queue.h>

#include "../include/log.h"
#include "../include/argtable3.h"

////==========================================================================
//// SETTINGS
////==========================================================================
#define SLEEP_TIME              1
#define DEFAULT_LOGGER_LEVEL    LOG_INFO
#define DEBUG_LOGGER_LEVEL      LOG_DEBUG
#define MAX_NAME_SIZE           10
#define MAX_MESSAGE_SIZE        100

#define TOKEN_TYPE_EMPTY        1
#define TOKEN_TYPE_MESSAGE      2



////==========================================================================
//// TYPES
////==========================================================================
typedef struct message {
    char from[MAX_NAME_SIZE];
    char to[MAX_NAME_SIZE];
    char msg[MAX_MESSAGE_SIZE];
} message;

union payload {
    message msg;
};

typedef struct token {
    char type;
    int uuid;
    union payload data;
} token;



////==========================================================================
//// QUEUE
////==========================================================================
struct queue_entry {
    char type;
    union payload data;
    TAILQ_ENTRY(queue_entry) entries;
};

TAILQ_HEAD(, queue_entry) queue_head;

void queue_add(char type, union payload *data) {

    // allocate memory
    struct queue_entry *elem;
    elem = malloc(sizeof(struct queue_entry));
    if (elem == NULL)
        exit_fatal_no("Failed to allocate memory for queue");

    // copy data
    elem->type = type;
    memcpy(&(elem->data), data, sizeof(union payload));

    // insert element
    TAILQ_INSERT_TAIL(&queue_head, elem, entries);
}

int queue_is_empty() {
    return TAILQ_EMPTY(&queue_head);
}

void queue_pop(char *type, union payload *data) {
    struct queue_entry *p = TAILQ_FIRST(&queue_head);
    TAILQ_REMOVE(&queue_head, p, entries);

    memcpy(type, &(p->type), sizeof(char));
    memcpy(data, &(p->data), sizeof(union payload));

    free(p);
}

void queue_init(){
    TAILQ_INIT(&queue_head);
}


////==========================================================================
//// ARGUMENTS
////==========================================================================
struct arg_lit *arg_help, *arg_token, *arg_debug;
struct arg_str *arg_name, *arg_next_ip;
struct arg_int *arg_port, *arg_next_port;
struct arg_end *end;

void parse_arguments(int argc, char **args) {

    void *argtable[] = {
            arg_help        = arg_litn("h", "help", 0, 1, "Display this help and exit"),
            arg_token       = arg_litn("t", "token", 0, 1, "Start with token"),
            arg_debug       = arg_litn("d", "debug", 0, 1, "Enable debug mode"),
            arg_name        = arg_strn(NULL, NULL, "<name>", 1, 1, "Name of this client"),
            arg_port        = arg_intn(NULL, NULL, "<port>", 1, 1, "Port for this client to listen on"),
            arg_next_ip     = arg_strn(NULL, NULL, "<next ip>", 1, 1, "IPv4 address of the next client"),
            arg_next_port   = arg_intn(NULL, NULL, "<next port>", 1, 1, "Port of the next client"),
            end             = arg_end(20)
    };

    // parse cmd line
    int errors = arg_parse(argc,args,argtable);

    // display help page if needed
    if (arg_help->count > 0) {
        printf("Usage: %s", args[0]);
        arg_print_syntax(stdout, argtable, "\n");
        printf("Arguments:\n");
        arg_print_glossary(stdout, argtable, "  %-25s %s\n");
        exit(0);
    }

    // display errors
    if (errors > 0) {
        arg_print_errors(stdout, end, args[0]);
        printf("Try '%s --help' for more information.\n", args[0]);
        exit(0);
    }

    // check name length
    if (strlen(arg_name->sval[0]) > MAX_NAME_SIZE)
        exit_error("Name too long, max size: %d", MAX_NAME_SIZE);

    // free memory
    arg_freetable(argtable, sizeof(argtable) / sizeof(argtable[0]));

}



////==========================================================================
//// LOGGER
////==========================================================================
pthread_mutex_t mutex_log = PTHREAD_MUTEX_INITIALIZER;

void logger_lock(void *udata, int lock){
    if(lock)
        pthread_mutex_lock(udata);
    else
        pthread_mutex_unlock(udata);
}

void setup_logger(){
    log_set_udata(&mutex_log);
    log_set_lock(logger_lock);
    log_set_level(arg_debug->count > 0 ? DEBUG_LOGGER_LEVEL : DEFAULT_LOGGER_LEVEL);
}



////==========================================================================
//// SOCKETS
////==========================================================================
int input_socket_fd;
int output_socket_fd;

void setup_input_socket() {

    // setup input address
    struct sockaddr_in in_address;
    memset(&in_address, 0, sizeof(in_address));
    in_address.sin_family = AF_INET;
    in_address.sin_addr.s_addr = INADDR_ANY;
    in_address.sin_port = htons(arg_port->ival[0]);

    // create socket
    input_socket_fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (input_socket_fd < 0)
        exit_fatal_no("An error occurred while creating of input socket");

    // bind socket
    if (bind(input_socket_fd, (const struct sockaddr *)&in_address, sizeof(in_address)) < 0)
        exit_fatal_no("An error occurred while binding of input socket");

    log_trace("Input socket bound successfully");

}

void setup_output_socket() {

    // setup next address
    struct sockaddr_in next_address;
    memset(&next_address, 0, sizeof(next_address));
    next_address.sin_family = AF_INET;
    next_address.sin_port = htons(arg_next_port->ival[0]);

    // parse ip
    if (inet_pton(AF_INET, arg_next_ip->sval[0], &(next_address.sin_addr)) != 1)
        exit_error("Next address not in IPv4 format");

    // create socket
    output_socket_fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (output_socket_fd < 0)
        exit_fatal_no("An error occurred while creating of output socket");

    // connect socket
    if (connect(output_socket_fd, (const struct sockaddr *)&next_address, sizeof(next_address)) == -1)
        exit_fatal_no("An error occurred while connecting to next client");

    log_trace("Output socket connected successfully");

}

void cleanup_input_socket() {
    if (close(input_socket_fd) == -1)
        log_error("An error occurred while closing input socket");

    log_trace("Input socket closed successfully");
}

void cleanup_output_socket() {
    if (close(output_socket_fd) == -1)
        log_error("An error occurred while closing output socket");

    log_trace("Output socket closed successfully");
}



////==========================================================================
//// CONSOLE
////==========================================================================
void handle_console_input() {

    log_info("Enter message in format: <client name> < <message>");

    while(1) {

        // create payload
        union payload data;

        // read console input
        scanf("%s < %[^\n]", data.msg.to, data.msg.msg);

        // set sender
        strcpy(data.msg.from, arg_name->sval[0]);

        // add to queue
        queue_add(TOKEN_TYPE_MESSAGE, &data);

        log_trace("Message \"%s\" for client \"%s\" added to queue", data.msg.msg, data.msg.to);
    }
}



////==========================================================================
//// OUTPUT SOCKET HANDLING
////==========================================================================
void send_token(struct token *tok) {
    sleep(SLEEP_TIME);

    if (write(output_socket_fd, tok, sizeof(struct token)) == -1)
        exit_fatal_no("An error occurred while sending token");

    log_debug("Token of type %d has been sent (uuid=%d)", tok->type, tok->uuid);
}

void send_new_token() {
    struct token tok;
    tok.type = TOKEN_TYPE_EMPTY;
    tok.uuid = rand();

    log_debug("Created new token of type %d (uuid=%d)", tok.type, tok.uuid);

    send_token(&tok);
}



////==========================================================================
//// INPUT SOCKET HANDLING
////==========================================================================
void handle_token_empty(struct token *tok) {

    // if there is something to send copy it into token
    if (!queue_is_empty())
        queue_pop(&(tok->type), &(tok->data));

    // send token
    send_token(tok);

}

void handle_token_message(struct token *tok) {

    log_debug("Received message token from %s to %s", tok->data.msg.from, tok->data.msg.to);

    // if message is for this client
    if (strcmp(tok->data.msg.to, arg_name->sval[0]) == 0) {

        // display message
        printf("%s > %s\n", tok->data.msg.from, tok->data.msg.msg);

        // send an empty token
        tok->type = TOKEN_TYPE_EMPTY;
        send_token(tok);

        return;
    }

    // if this is token from this client (loop detection)
    if (strcmp(tok->data.msg.from, arg_name->sval[0]) == 0) {

        // display error
        log_warn("Message could not be delivered to \"%s\" - no such client in network", tok->data.msg.to);

        // send an empty token
        tok->type = TOKEN_TYPE_EMPTY;
        send_token(tok);

        return;
    }

    // message not related to this client
    send_token(tok);

}

void *handle_input_socket() {

    log_trace("Listening on input socket");

    while(1){

        // read token
        struct token tok;
        if (read(input_socket_fd, &tok, sizeof(struct token)) == -1)
            exit_fatal_no("An error while reading token from input socket");

        // log
        log_debug("Received token of type %d (uuid=%d)", tok.type, tok.uuid);

        // handle token
        switch (tok.type) {
            case TOKEN_TYPE_EMPTY:
                handle_token_empty(&tok);
                break;
            case TOKEN_TYPE_MESSAGE:
                handle_token_message(&tok);
                break;
            default:
                log_error("Received token of unknown type %d (uuid=%d)", tok.type, tok.uuid);
        }
    }
}

void setup_input_socket_thread() {
    pthread_t tid;
    if (pthread_create(&tid, NULL, handle_input_socket, NULL) != 0)
        exit_fatal_no("An error occurred while creating input socket thread");

    log_trace("Input socket thread created");
}



////==========================================================================
//// CLEANUP
////==========================================================================
void handle_exit() {
    cleanup_input_socket();
    cleanup_output_socket();
}

void setup_atexit() {
    if (atexit(handle_exit) != 0)
        exit_fatal("An error occurred while setting up atexit function");

    log_trace("At exit handler registered");
}

void handle_sigint() {
    log_debug("Received SIGINT - terminating client");
    exit(0);
}

void setup_sigint() {
    struct sigaction sa;
    sa.sa_handler = handle_sigint;
    sigemptyset(&sa.sa_mask);
    if (sigaction(SIGINT, &sa, NULL) == -1)
        exit_fatal_no("An error occurred while setting up handler for SIGINT");

    log_trace("SIGINT handler registered");
}



////==========================================================================
//// MAIN
////==========================================================================
int main(int argc, char **args) {
    srand(time(NULL));

    parse_arguments(argc, args);

    setup_logger();

    setup_atexit();
    setup_sigint();

    queue_init();

    setup_input_socket();
    setup_output_socket();

    setup_input_socket_thread();

    if (arg_token->count)
        send_new_token();

    handle_console_input();

    return 0;
}
