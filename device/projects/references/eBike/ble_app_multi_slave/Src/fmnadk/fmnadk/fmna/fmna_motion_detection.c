


#include "fmna_util.h"
#include "fmna_motion_detection.h"
#include "fmna_state_machine.h"
#include "fmna_constants.h"



#define SEPARATED_UT_MOTION_SAMPLING_RATE1_MS             SEC_TO_MSEC(10)    // Passive poll rate
#define SEPARATED_UT_MOTION_SAMPLING_RATE2_MS             500                // Active poll rate
#define SEPARATED_UT_MOTION_ACTIVE_POLL_DURATION_MS       SEC_TO_MSEC(20)
#define SEPARATED_UT_MOTION_BACKOFF_MS                    HOURS_TO_MSEC(6) //6 hours
#define SEPARATED_UT_MOTION_MAX_SOUND_COUNT               10
#define SEPARATED_UT_MOTION_MAX_DET_COUNT                 50

static bool     m_motion_active_polling_enabled = false;
static bool     m_motion_active_polling_ended   = false;
static uint16_t m_motion_check_poll_rate_ms     = SEPARATED_UT_MOTION_SAMPLING_RATE1_MS;
static uint8_t  m_motion_sound_count            = 0;
static uint8_t  m_motion_det_count              = 0;

static fmna_timer_handle_t m_motion_poll_timer_id;

static fmna_timer_handle_t m_motion_active_poll_duration_timer_id;

static uint32_t m_motion_backoff_timeout_ms = SEPARATED_UT_MOTION_BACKOFF_MS;
static fmna_timer_handle_t m_motion_backoff_timer_id;



static void motion_active_poll_duration_timeout_sched_handler(void *p_event_data, uint16_t event_size)
{
    // Haven't detected movement for active polling duration. Turn off motion detection
    // and start the backoff timer so that we only reinitialize after X hours.
    bool ret_code;


    fmna_timer_stop(&m_motion_poll_timer_id);

    m_motion_active_polling_enabled = false;
    m_motion_active_polling_ended   = true;

    FMNA_LOG_INFO("Start backoff timer (%d ms)", m_motion_backoff_timeout_ms);
    ret_code = fmna_timer_start(&m_motion_backoff_timer_id, m_motion_backoff_timeout_ms);
    FMNA_BOOL_CHECK(ret_code);
}

static void motion_poll_timer_timeout_handler(void)
{
    m_motion_det_count++;
    // Check if we detect motion, e.g. orientation change.
    if (fmna_motion_detection_platform_is_motion_detected())
    {
        fmna_state_machine_dispatch_event(FMNA_SM_EVENT_MOTION_DETECTED);
        m_motion_sound_count++;
    }

    if (m_motion_det_count >= SEPARATED_UT_MOTION_MAX_DET_COUNT)
    {
        // Max motion detect , we should preemptively not restart poll timer and go into backoff mode..
        FMNA_LOG_INFO("Max motion detection. Stopping polling, going to backoff.");

        // Stop the polling timer.
        fmna_timer_stop(&m_motion_active_poll_duration_timer_id);

        // Schedule the handler to cleanup and go into motion detection backoff.
        fmna_queue_platform_evt_put(NULL, 0, motion_active_poll_duration_timeout_sched_handler);
    }
}

static void motion_backoff_timeout_handler(void)
{
    // Backoff period is over, and we are still in Separated. Re-init motion detection.
    FMNA_LOG_INFO("Backoff period is over, and we are still in Separated. Re-init motion detection");
    fmna_motion_detection_start();
}

static void motion_active_poll_duration_timer_timeout_handler(void)
{
    // Schedule poll timer stop and blackout timer start.
    fmna_queue_platform_evt_put(NULL, 0, motion_active_poll_duration_timeout_sched_handler);
}

void fmna_motion_detection_init(void)
{
    bool ret_code = FMNA_SUCCESS;

    ret_code = fmna_timer_create(&m_motion_poll_timer_id, true, motion_poll_timer_timeout_handler);
    FMNA_BOOL_CHECK(ret_code);
    ret_code = fmna_timer_create(&m_motion_active_poll_duration_timer_id, false, motion_active_poll_duration_timer_timeout_handler);
    FMNA_BOOL_CHECK(ret_code);
    ret_code = fmna_timer_create(&m_motion_backoff_timer_id, false, motion_backoff_timeout_handler);
    FMNA_BOOL_CHECK(ret_code);
}

static void stop_all_timers(void)
{
    fmna_timer_stop(&m_motion_poll_timer_id);

    // Stop the active poll duration timer, if applicable
    fmna_timer_stop(&m_motion_active_poll_duration_timer_id);

    // Stop the accel backoff timer, if applicable
    fmna_timer_stop(&m_motion_backoff_timer_id);
}



void fmna_motion_detection_start_active_polling(void)
{
    // initialize to active poll rate
    m_motion_check_poll_rate_ms = SEPARATED_UT_MOTION_SAMPLING_RATE2_MS;

    // Check if active poll duration completed during motion detection play sound.
    if (!m_motion_active_polling_ended)
    {
        if (m_motion_sound_count >= SEPARATED_UT_MOTION_MAX_SOUND_COUNT)
        {
            // Max motion detect sounds played, we should preemptively not restart poll timer and go into backoff mode..
            FMNA_LOG_INFO("Max motion detection sounds played. Stopping polling, going to backoff.");

            // Stop the polling timer.
            fmna_timer_stop(&m_motion_active_poll_duration_timer_id);

            // Schedule the handler to cleanup and go into motion detection backoff.
            fmna_queue_platform_evt_put(NULL, 0, motion_active_poll_duration_timeout_sched_handler);
        }
        else
        {
            // We are still active polling... (re)start polling.
            fmna_timer_stop(&m_motion_poll_timer_id);
            fmna_timer_start(&m_motion_poll_timer_id, m_motion_check_poll_rate_ms);

            if (!m_motion_active_polling_enabled)
            {
                // Start active polling for the first time.
                // If we are currently active polling, don't restart the timer.
                FMNA_LOG_INFO("Start active polling to detect motion (%d ms)", SEPARATED_UT_MOTION_ACTIVE_POLL_DURATION_MS);
                bool ret = fmna_timer_start(&m_motion_active_poll_duration_timer_id, SEPARATED_UT_MOTION_ACTIVE_POLL_DURATION_MS);
                FMNA_BOOL_CHECK(ret);
                m_motion_active_polling_enabled = true;
            }
        }
    }
}


void fmna_motion_detection_stop(void)
{
    stop_all_timers();

    fmna_motion_detection_platform_stop();
}

void fmna_motion_detection_start(void)
{
    // Initialize the poll rate
    m_motion_check_poll_rate_ms = SEPARATED_UT_MOTION_SAMPLING_RATE1_MS;

    // Reset state
    m_motion_active_polling_ended   = false;
    m_motion_active_polling_enabled = false;
    m_motion_sound_count   = 0;
    m_motion_det_count     = 0;

    fmna_motion_detection_platform_start();

    // Start passive polling to detect motion
    FMNA_LOG_INFO("Start passive polling to detect motion (%d ms)", m_motion_check_poll_rate_ms);
    bool ret_code = fmna_timer_start(&m_motion_poll_timer_id, m_motion_check_poll_rate_ms);
    FMNA_BOOL_CHECK(ret_code);
}

#if FMNA_DEBUG_ENABLE
void fmna_motion_detection_set_separated_ut_backoff_timeout_seconds(uint32_t separated_ut_backoff_timeout_seconds)
{
    m_motion_backoff_timeout_ms = SEC_TO_MSEC(separated_ut_backoff_timeout_seconds);
}
#endif //DEBUG
