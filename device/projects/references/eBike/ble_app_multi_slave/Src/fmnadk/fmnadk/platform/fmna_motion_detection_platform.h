/*
*      Copyright (C) 2020 Apple Inc. All Rights Reserved.
*
*      Find My Network ADK is licensed under Apple Inc.’s MFi Sample Code License Agreement,
*      which is contained in the License.txt file distributed with the Find My Network ADK,
*      and only to those who accept that license.
*/

#ifndef fmna_motion_detection_platform_h
#define fmna_motion_detection_platform_h


#include "fmna_application.h"

fmna_ret_code_t fmna_motion_detection_platform_init(fmna_motion_detect_start_t  motion_detect_start,
                                                    fmna_motion_detect_stop_t   motion_detect_stop,
                                                    fmna_motion_is_detected_t   is_motion_detected);
void fmna_motion_detection_platform_start(void);
void fmna_motion_detection_platform_stop(void);
bool fmna_motion_detection_platform_is_motion_detected(void);

#endif /* fmna_motion_detection_platform_h */
