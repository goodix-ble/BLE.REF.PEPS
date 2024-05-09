#include "rssi_check.h"
#include "user_gui.h"
#include <string.h>
#include "stdio.h"

#define RSSI_SAVE_COUNT   20

static int16_t s_rssi_buffer[RSSI_SAVE_COUNT];
static uint8_t s_save_count = 0;

static bool s_lock_open = false;



bool lock_if_open(void)
{
    return s_lock_open;
}

void lock_open(bool if_open)
{
    s_lock_open = if_open;
    if(if_open)
    {
        show_open();
    }
    else
    {
        show_close();
    }
}


//开锁条件，连接上，靠近，达到一定值
//闭锁条件，断连
//远离，达到一定值
static void rssi_value_check(void)
{
    int16_t sum = 0;
    int16_t average = 0;
    for(uint8_t i=0; i< RSSI_SAVE_COUNT; i++)//先求平均数
    {
        sum += s_rssi_buffer[i];
    }
    average = sum / RSSI_SAVE_COUNT;
    printf("average = %d\r\n", average);
    if(s_lock_open)
    {
        if(average < -70)
        {
            lock_open(false);
        }
    }
    else
    {
        if(average > -50)
        {
            lock_open(true);
        }
    }
}

void rssi_clear(void)
{
    s_save_count = 0;
}

void rssi_value_add(int16_t rssi_value)
{
    for(uint8_t i=RSSI_SAVE_COUNT-1; i>0; i--)
    {
        s_rssi_buffer[i] = s_rssi_buffer[i-1];
    }
    s_rssi_buffer[0] = rssi_value;
    
    if(s_save_count < RSSI_SAVE_COUNT)
    {
        s_save_count++;
    }
    else
    {
        rssi_value_check();
    }
}
