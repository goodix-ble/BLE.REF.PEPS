#include "multi_slave_info.h"
#include "gr55xx_sys.h"
#include "multi_slave_config.h"


static multi_slave_info_t  s_salves[MAX_SLAVE_INDEX] = ALL_SLAVE_DEVICES;


/*---------------------------------------------------------------------*/
static bool multi_slave_addr_cmp(const uint8_t *addr, const uint8_t *compare_addr)
{
    uint8_t i = 0;
    
    while(i < 6)
    {
        if (addr[i] != compare_addr[i])
        {
            break;
        }
        i++;
    }

    if (i == 6)
    {
        return 1;
    }
    else
    {
        return 0;
    }
}

devices_info_t* devices_get_info(void)
{
    return devices;
}
/*---------------------------------------------------------------------*/
void devices_info_init(void)
{
    uint8_t send_data[28];
    uint8_t reset_flag = 0;
    uint16_t len = 1; 
    
    nvds_get(NV_TAG_APP(0x05), &len, &reset_flag);

    for(uint8_t i=0; i<MAX_MANAGER_DEVICES; i++)
    {
        len = sizeof(devices_info_t);
        nvds_get(devicess_nvds_tag[i], &len, (uint8_t*)&devices[i]); 
    }
    
    if(reset_flag == 0x11)// reset device 
    {
        cm_send_frame(send_data,0,0x0001);
        reset_flag = 0;
        nvds_put(NV_TAG_APP(0x05), 1, &reset_flag);
    }
    else if(reset_flag == 0x22)// set device info
    {
        memcpy(send_data, devices, 28);
        cm_send_frame(send_data, 28, 0x0002);
        reset_flag = 0;
        nvds_put(NV_TAG_APP(0x05), 1, &reset_flag);
    }
}

/*---------------------------------------------------------------------*/
void devices_info_get(uint8_t *devices_info)
{
    memcpy(devices_info, devices, sizeof(devices));
}

void devices_info_update(uint8_t *devices_info)
{
    memcpy(devices, devices_info, sizeof(devices));
    for(uint8_t i=0; i<MAX_MANAGER_DEVICES; i++)
    {
        devices_info_save(i);
    }
}

/*---------------------------------------------------------------------*/
bool devices_info_find_addr(const uint8_t *addr, uint8_t *p_out_index)
{
    for(uint8_t i=0; i<MAX_MANAGER_DEVICES; i++)
    {
        if(devices[i].type != DEV_INVALID)
        {
            if(devices_addr_cmp(addr, devices[i].addr))
            {
                *p_out_index = i;
                return true;
            }
        }
    }
    return false;
}
