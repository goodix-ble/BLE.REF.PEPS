
#include "fmna_motion_detection_platform.h"
#include "fmna_log_platform.h"


static fmna_motion_detect_start_t   s_motion_detect_start;
static fmna_motion_detect_stop_t    s_motion_detect_stop;
static fmna_motion_is_detected_t    s_is_motion_detected;


fmna_ret_code_t fmna_motion_detection_platform_init(fmna_motion_detect_start_t  motion_detect_start,
                                                    fmna_motion_detect_stop_t   motion_detect_stop,
                                                    fmna_motion_is_detected_t   is_motion_detected)
{
    s_motion_detect_start = motion_detect_start;
    s_motion_detect_stop  = motion_detect_stop;
    s_is_motion_detected  = is_motion_detected;

    return FMNA_SUCCESS;
}

void fmna_motion_detection_platform_start(void)
{
    if (s_motion_detect_start)
    {
        s_motion_detect_start();
    }
    else
    {
         FMNA_LOG_INFO("No [MOTION DETECT] module, [ACTION]: start motion detect [power on]");
    }
}

void fmna_motion_detection_platform_stop(void)
{
    if (s_motion_detect_stop)
    {
        s_motion_detect_stop();
    }
    else
    {
        FMNA_LOG_INFO("No [MOTION DETECT] module, [ACTION]: stop motion detect [power off]");
    }
}

bool fmna_motion_detection_platform_is_motion_detected(void)
{
    if (s_is_motion_detected)
    {
        return s_is_motion_detected();
    }
    else
    {
        FMNA_LOG_INFO("No [MOTION DETECT] module, [ACTION]: get motion sensor value and determine if it is moving");
    }

    return false;
}

