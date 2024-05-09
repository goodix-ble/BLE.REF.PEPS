


#ifndef __FINDMY_PLATFORM_PORT_H__
#define __FINDMY_PLATFORM_PORT_H__

#include "fmna_application.h"
#include "fmna_application_config.h"
#include "app_memory.h"
#include "app_queue.h"

#if FMNA_CUSTOM_BOARD_ENABLE
#include "custom_log_uart.h"
#else
#include "board_SK.h"
#endif

#include "hal_flash.h"

static uint8_t     s_fmna_queue_buffer[FNMA_QUEUE_ITEM_SIZE * 16];
static app_queue_t s_fmna_queue = 
{
    .element_size  = FNMA_QUEUE_ITEM_SIZE,
    .queue_size    = 15,
    .p_buffer      = s_fmna_queue_buffer,
};

// Queue port
static bool findmy_queue_send(void const *data, uint16_t size, bool in_isr)
{
    return app_queue_push(&s_fmna_queue, data) ? false : true;
}


static bool findmy_queue_receive(void const *buffer, uint16_t size)
{
    return app_queue_pop(&s_fmna_queue, (void *)buffer) ? false : true;
}

// Log output port
static void findmy_log_output(uint8_t *data, uint16_t length)
{
#if FMNA_CUSTOM_BOARD_ENABLE
    custom_log_uart_send(data, length);
#else
    bsp_uart_send(data, length);
#endif
}

// Memory malloc port
static void* findmy_mem_malloc(size_t size)
{
    return app_malloc(size);
}

static void* findmy_mem_realloc(void *ptr, size_t size)
{
    return app_realloc(ptr, size);
}

static void findmy_mem_free(void *ptr) 
{
    app_free(ptr);
}

// Flash storage port
static bool findmy_flash_init(void)
{
    return 1;
}

static uint32_t findmy_flash_read(const uint32_t addr, uint8_t *buf, const uint32_t size)
{
    return hal_flash_read(addr, buf, size);
}

static uint32_t findmy_flash_write(const uint32_t addr, const uint8_t *buf, const uint32_t size)
{
    return hal_flash_write(addr, buf, size);
}
static bool findmy_flash_erase(const uint32_t addr, const uint32_t size)
{
    return hal_flash_erase(addr, size);
}





#endif









