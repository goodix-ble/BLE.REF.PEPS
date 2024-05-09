#ifndef fmna_queue_platform_h
#define fmna_queue_platform_h

#include "fmna_application.h"

typedef void (*fmna_platform_evt_handler_t)(void *p_data, uint16_t data_size);

typedef struct
{
    fmna_platform_evt_handler_t  evt_handler;
    void                        *p_data;
    uint16_t                     data_size;
} __attribute__((packed))  fmna_task_evt_t;



fmna_ret_code_t fmna_queue_platform_init(fmna_queue_send_t queue_send, fmna_queue_receive_t queue_receive);
fmna_ret_code_t fmna_queue_platform_evt_put(void *p_evt_data, uint16_t evt_data_size, fmna_platform_evt_handler_t evt_handler);
fmna_ret_code_t fmna_queue_platform_evt_get(fmna_task_evt_t *p_evt);
#endif
