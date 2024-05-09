

#include "fmna_timer_platform.h"
#include "fmna_log_platform.h"


#define FMNA_TIMER_ONECE_DELAY_MAX   (60 * 60 * 1000)

static bool fmna_timer_start_continue(fmna_timer_handle_t *p_handle, uint32_t delay)
{
    p_handle->remain_delay  = delay;
    p_handle->current_delay = delay > FMNA_TIMER_ONECE_DELAY_MAX ? FMNA_TIMER_ONECE_DELAY_MAX : delay;

    return app_timer_start(p_handle->timer_id, p_handle->current_delay, p_handle) ? false : true;
}

static void fmna_timer_timeout_handler(void* p_ctx)
{
    bool ret;
    fmna_timer_handle_t *handle_ptr = (fmna_timer_handle_t*)p_ctx;

    handle_ptr->remain_delay -= handle_ptr->current_delay;

    if (handle_ptr->remain_delay)
    {
        ret = fmna_timer_start_continue(handle_ptr, handle_ptr->remain_delay);
        FMNA_BOOL_CHECK(ret);
    }
    else
    {
        handle_ptr->timer_cb();
        if (handle_ptr->is_auto_reload)
        {
            handle_ptr->remain_delay  = handle_ptr->total_delay;
            handle_ptr->current_delay = handle_ptr->total_delay > FMNA_TIMER_ONECE_DELAY_MAX ? FMNA_TIMER_ONECE_DELAY_MAX : handle_ptr->total_delay;
            ret = app_timer_start(handle_ptr->timer_id, handle_ptr->current_delay, handle_ptr) ? false : true;
            FMNA_BOOL_CHECK(ret);
        }
    }
}

bool fmna_timer_create(fmna_timer_handle_t *p_handle, bool auto_reload, fmna_timer_cb_t cb)
{
    if (NULL == cb)
    {
        return false;
    }

    p_handle->timer_cb       = cb;
    p_handle->is_auto_reload = auto_reload;

    return app_timer_create(&p_handle->timer_id, ATIMER_ONE_SHOT, fmna_timer_timeout_handler) ? false : true;
}

bool fmna_timer_start(fmna_timer_handle_t *p_handle, uint32_t delay)
{
    p_handle->total_delay   = delay;
    p_handle->remain_delay  = delay;
    p_handle->current_delay = delay > FMNA_TIMER_ONECE_DELAY_MAX ? FMNA_TIMER_ONECE_DELAY_MAX : delay;

    return app_timer_start(p_handle->timer_id, p_handle->current_delay, p_handle) ? false : true;
}

void fmna_timer_stop(fmna_timer_handle_t *p_handle)
{
    app_timer_stop(p_handle->timer_id);
}
