#ifndef _LONG_TERM_USER_H_
#define _LONG_TERM_USER_H_

#include <stdint.h>
#include <stdbool.h>

void hid_name_save(uint8_t *name, uint8_t len);
void nvds_info_init(void);
void save_share_key(uint8_t *p_key, uint8_t len);
void save_user_id(uint8_t *p_user_id, uint8_t len);
bool get_bonded_flag(void);
void clear_nvds_info(void);
uint8_t *get_share_key(void);
uint8_t *get_user_id(void);
bool bond_info_check(void);

#endif

