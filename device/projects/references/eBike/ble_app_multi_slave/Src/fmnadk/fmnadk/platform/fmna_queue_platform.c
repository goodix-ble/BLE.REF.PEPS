


#include "fmna_queue_platform.h"
#include "fmna_malloc_platform.h"
#include "fmna_log_platform.h"


static fmna_queue_send_t            s_queue_send;
static fmna_queue_receive_t         s_queue_receive;




static fmna_task_evt_t s_fmna_event;

fmna_ret_code_t fmna_queue_platform_init(fmna_queue_send_t queue_send, fmna_queue_receive_t queue_receive)
{
    if (NULL == queue_send || NULL == queue_receive)
    {
        return FMNA_ERROR_INVALID_QUEUE_PARAM;
    }

    FMNA_BOOL_CHECK((FNMA_QUEUE_ITEM_SIZE == sizeof(fmna_task_evt_t)));
    
    s_queue_send    = queue_send;
    s_queue_receive = queue_receive;

    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_queue_platform_evt_put(void *p_evt_data, uint16_t evt_data_size, fmna_platform_evt_handler_t evt_handler)
{
    if (s_queue_send == NULL)
    {
        return FMNA_ERROR_NULL;
    }

    memset(&s_fmna_event, 0, sizeof(s_fmna_event));

    if (evt_data_size && p_evt_data)
    {
        s_fmna_event.p_data  = fmna_malloc(evt_data_size);

        if (NULL == s_fmna_event.p_data)
        {
            return FMNA_ERROR_INTERNAL;
        }

        memcpy(s_fmna_event.p_data, p_evt_data, evt_data_size);
        s_fmna_event.data_size   = evt_data_size;
    }

    s_fmna_event.evt_handler = evt_handler;

    return s_queue_send(&s_fmna_event, sizeof(s_fmna_event), ((__get_IPSR() & 0xFF) != 0)) ? FMNA_SUCCESS : FMNA_ERROR_INTERNAL;
}


fmna_ret_code_t fmna_queue_platform_evt_get(fmna_task_evt_t *p_evt)
{
    if (s_queue_receive == NULL || NULL == p_evt)
    {
        return FMNA_ERROR_NULL;
    }

    if (!s_queue_receive(p_evt, sizeof(fmna_task_evt_t)))
    {
        return FMNA_ERROR_INTERNAL;
    }

    return FMNA_SUCCESS;
}




