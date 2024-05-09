

#include "fmna_sound_platform.h"
#include "fmna_log_platform.h"
#include "fmna_timer_platform.h"

#include "fmna_state_machine.h"
#include "fmna_util.h"

static fmna_speaker_on_t       s_speaker_on;
static fmna_speaker_off_t      s_speaker_off;

static fmna_timer_handle_t   m_fmna_sound_timeout_timer_id;   /** Sound timeout timer id*/


static void fmna_sound_timeout_handler(void)
{
    if (s_speaker_off)
    {
        s_speaker_off();
    } 
    else
    {
        FMNA_LOG_INFO("No [SPEAKER] module, [ACTION]: stop play sound");
    }

    fmna_state_machine_dispatch_event(FMNA_SM_EVENT_SOUND_COMPLETE);
}

fmna_ret_code_t fmna_sound_platform_init(fmna_speaker_on_t   speaker_on,
                                         fmna_speaker_off_t  speaker_off)
{
    bool ret;

    s_speaker_on   = speaker_on;
    s_speaker_off  = speaker_off;


    ret = fmna_timer_create(&m_fmna_sound_timeout_timer_id, false, fmna_sound_timeout_handler);
    FMNA_BOOL_CHECK(ret);

    return FMNA_SUCCESS;
}

void fmna_sound_platform_found_start(void)
{
    bool ret = fmna_timer_start(&m_fmna_sound_timeout_timer_id, SEC_TO_MSEC(10));
    FMNA_BOOL_CHECK(ret);

    if (!ret)
    {
        return;
    }

    if (s_speaker_on)
    {
        s_speaker_on(FMNA_SPEAKER_ON_START_FOUND);
    }
    else
    {
        FMNA_LOG_INFO("No [SPEAKER] module, [ACTION]: play sound [FMNA_SPEAKER_ON_START_FOUND]");
    }
}

void fmna_sound_platform_found_stop(void)
{
    fmna_timer_stop(&m_fmna_sound_timeout_timer_id);

    if (s_speaker_off)
    {
        s_speaker_off();
    }
    else
    {
        FMNA_LOG_INFO("No [SPEAKER] module, [ACTION]: stop play sound");
    }

    fmna_state_machine_dispatch_event(FMNA_SM_EVENT_SOUND_COMPLETE);
}

void fmna_sound_platform_paired(void)
{
    if (s_speaker_on)
    {
        s_speaker_on(FMNA_SPEAKER_ON_PAIRED);
    }
    else
    {
        FMNA_LOG_INFO("No [SPEAKER] module, [ACTION]: play sound [FMNA_SPEAKER_ON_PAIRED]");
    }
}


void fmna_sound_platform_unpair(void)
{
    if (s_speaker_on)
    {
        s_speaker_on(FMNA_SPEAKER_ON_UNPAIR);
    }
    else
    {
        FMNA_LOG_INFO("No [SPEAKER] module, [ACTION]: play sound [FMNA_SPEAKER_ON_UNPAIR]");
    }
}

