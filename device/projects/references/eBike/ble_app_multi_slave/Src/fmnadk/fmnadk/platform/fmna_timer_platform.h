#ifndef fmna_timer_platform_h
#define fmna_timer_platform_h

#include "fmna_application.h"
#include "app_timer.h"

typedef void (*fmna_timer_cb_t)(void);

typedef struct
{
    app_timer_id_t   timer_id;
    fmna_timer_cb_t  timer_cb;
    uint32_t         total_delay;
    uint32_t         remain_delay;
    uint32_t         current_delay;
    bool             is_auto_reload;
} fmna_timer_handle_t;



bool fmna_timer_create(fmna_timer_handle_t *p_handle, bool auto_reload, fmna_timer_cb_t cb);
bool fmna_timer_start(fmna_timer_handle_t *p_handle, uint32_t delay);
void fmna_timer_stop(fmna_timer_handle_t *p_handle);

#endif
