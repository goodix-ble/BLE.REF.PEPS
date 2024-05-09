
#ifndef fmna_sound_platform_h
#define fmna_sound_platform_h

#include "fmna_application.h"

fmna_ret_code_t fmna_sound_platform_init(fmna_speaker_on_t   speaker_on,
                                         fmna_speaker_off_t  speaker_off);
void fmna_sound_platform_found_start(void);
void fmna_sound_platform_found_stop(void);
void fmna_sound_platform_paired(void);
void fmna_sound_platform_unpair(void);

#endif /* fmna_sound_platform_h */
