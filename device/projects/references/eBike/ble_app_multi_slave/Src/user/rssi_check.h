#ifndef _RSSI_CHECK_H_
#define _RSSI_CHECK_H_

#include <stdint.h>
#include <stdbool.h>


typedef enum
{
    PEPS_CONNECT_STATE,
    PEPS_DISCONNECT_SATE,
    PEPS_NEAR_STATE,
    PEPS_AWAY_STATE,
}peps_state_t;

void rssi_value_add(int16_t rssi_value);
void rssi_clear(void);
void lock_open(bool if_open);
bool lock_if_open(void);

#endif

