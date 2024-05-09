


#include "gr5xxx_rng.h"
#include "gr_includes.h"


static rng_handle_t s_rng_handle =
{
    .p_instance = RNG,
    .init.seed_mode  = RNG_SEED_FR0_S0,
    .init.lfsr_mode  = RNG_LFSR_MODE_59BIT,
    .init.out_mode   = RNG_OUTPUT_FR0_S0,
    .init.post_mode  = RNG_POST_PRO_NOT
};


int gr5xxx_random_generate(byte* output, word32 sz)
{
    int remaining = sz;
    int pos = 0;
    hal_status_t ret = (hal_status_t)0;

    if (HAL_OK != hal_rng_init(&s_rng_handle))
    {
        return -1;
    }

    while (remaining > 0)
    {
        uint32_t out_data;
        ret = hal_rng_generate_random_number(&s_rng_handle, NULL, &out_data);
        if (HAL_OK != ret)
        {
            break;
        }
        output[pos] = out_data &0xff;
        remaining -= 1;
        pos ++;
    }

    hal_rng_deinit(&s_rng_handle);

    return (ret == HAL_OK) ? 0 : -1;
}



