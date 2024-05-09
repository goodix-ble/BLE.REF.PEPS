


#include "gr5xxx_ecc.h"

#include "custom_config.h"

#include "gr55xx_ll_pkc.h"
#include "gr55xx_hal_def.h"
#include "crypto_ecc.h"


#define LL_ECC_CURVE_SECP224R1_CONFIG                                                                             \
{                                                                                                                 \
    .A        = {0x00000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFC, 0xFFFFFFFF, 0x00000000, 0x00000003, 0x00000001}, \
    .B        = {0x00000000, 0x7FC02F93, 0x3DCEBA98, 0xC8528151, 0x107AC2F3, 0xCCF01310, 0xE768CDF6, 0x63C059CD}, \
    .P        = {0x00000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0x00000000, 0x00000000, 0x00000001}, \
    .PRSquare = {0x00000000, 0xFFFFFFFF, 0xFFFFFFFE, 0x00000000, 0xFFFFFFFF, 0x00000000, 0xFFFFFFFF, 0x00000001}, \
    .ConstP   =  0xFFFFFFFF,                                                                                      \
    .N        = {0x00000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFF16A2, 0xE0B8F03E, 0x13DD2945, 0x5C5C2A3D}, \
    .NRSquare = {0x00000000, 0xB1E97961, 0x6AD15F7C, 0xD9714856, 0xABC8FF59, 0x31D63F4B, 0x29947A69, 0x5F517D15}, \
    .ConstN   =  0x6A1FC2EB,                                                                                      \
    .H        =  1,                                                                                               \
    .G.X      = {0x00000000, 0xB70E0CBD, 0x6BB4BF7F, 0x321390B9, 0x4A03C1D3, 0x56C21122, 0x343280D6, 0x115C1D21}, \
    .G.Y      = {0x00000000, 0xBD376388, 0xB5F723FB, 0x4C22DFE6, 0xCD4375A0, 0x5A074764, 0x44D58199, 0x85007E34}, \
}

static void swap_endian(uint32_t *in, uint32_t len, uint32_t *out)
{
    uint32_t i = 0;

    for (i = 0; i < len; i++)
    {
        out[i] = ((in[i] & 0xFF000000) >> 24) + ((in[i] & 0xFF0000) >> 8)
                + ((in[i] & 0xFF00) << 8) + ((in[i] & 0xFF) << 24);
    }
}

int gr5xxx_ecc_make_pub_ex(ecc_key* key, int curve_id)
{
    int ret = 0;
    pkc_handle_t PKCHandle;
    ecc_curve_init_t ECC_P224_CurveInitStruct = LL_ECC_CURVE_SECP224R1_CONFIG;
    ecc_curve_init_t ECC_P256_CurveInitStruct = LL_ECC_CURVE_SECP256R1_CONFIG;
    pkc_ecc_point_multi_t PKC_ECCPointMulStruct;
    ecc_point_t Out = {{0}, {0}};
    uint8_t in_k[32] = {0};
    uint8_t Out_point[65] = {0};

    /* PKC init */
    PKCHandle.p_instance         = PKC;
    PKCHandle.p_result           = &Out;
    switch (curve_id)
    {
        case ECC_SECP224R1:
            PKCHandle.init.p_ecc_curve = &ECC_P224_CurveInitStruct;
            break;
        case ECC_SECP256R1:
            PKCHandle.init.p_ecc_curve = &ECC_P256_CurveInitStruct;
            break;
        default:
            return HAL_ERROR;
    }
    PKCHandle.init.data_bits   = 256;
    PKCHandle.init.secure_mode = PKC_SECURE_MODE_DISABLE;
    PKCHandle.init.random_func = (uint32_t (*)(void))rand;
    hal_pkc_deinit(&PKCHandle);
    hal_pkc_init(&PKCHandle);

    /* load private key into bytes */
    switch (curve_id)
    {
        case ECC_SECP224R1:
            mp_to_unsigned_bin_len(&(key->k), in_k + 4, 28);
            break;
        case ECC_SECP256R1:
            mp_to_unsigned_bin_len(&(key->k), in_k, 32);
            break;
        default:
            ret = HAL_ERROR;
            goto exit;
    }
    swap_endian((uint32_t *)in_k, 8, (uint32_t *)in_k);

    /* PKC start */
    PKC_ECCPointMulStruct.p_K = (uint32_t *)in_k;
    PKC_ECCPointMulStruct.p_ecc_point = NULL;

    ret = hal_pkc_ecc_point_multi(&PKCHandle, &PKC_ECCPointMulStruct, 5000);
    if (ret != HAL_OK)
    {
        goto exit;
    }

    swap_endian((uint32_t *)in_k, 8, (uint32_t *)in_k);
    swap_endian((uint32_t *)Out.X, 8, (uint32_t *)Out.X);
    swap_endian((uint32_t *)Out.Y, 8, (uint32_t *)Out.Y);

    switch (curve_id)
    {
        case ECC_SECP224R1:
            /* build secp224r1 output point */
            Out_point[0] = 0x4;
            memcpy(Out_point + 1, ((uint8_t *)Out.X) + 4, 28);
            memcpy(Out_point + 1 + 28, ((uint8_t *)Out.Y) + 4, 28);
            /* wolfssl import private key and public key */
            ret = wc_ecc_import_x963_ex(Out_point, 1 + 28 * 2, key, ECC_SECP224R1);
            if (ret != 0)
            {
                goto exit;
            }
            mp_read_unsigned_bin(&(key->k), in_k + 4, 28);
            break;
        case ECC_SECP256R1:
            /* build secp256r1 output point */
            Out_point[0] = 0x4;
            memcpy(Out_point + 1, ((uint8_t *)Out.X), 32);
            memcpy(Out_point + 1 + 32, ((uint8_t *)Out.Y), 32);
            /* wolfssl import private key and public key */
            ret = wc_ecc_import_x963_ex(Out_point, 1 + 32 * 2, key, ECC_SECP256R1);
            if (ret != 0)
            {
                goto exit;
            }
            mp_read_unsigned_bin(&(key->k), in_k, 32);
            break;
        default:
            ret = HAL_ERROR;
            goto exit;
    }

exit:
    memset(in_k, 0, sizeof(in_k));
    memset(Out_point, 0, sizeof(Out_point));
    memset((uint8_t *)Out.X, 0, sizeof(Out.X));
    memset((uint8_t *)Out.Y, 0, sizeof(Out.Y));
    hal_pkc_deinit(&PKCHandle);
    return ret;
}

int gr5xxx_ecc_shared_secret(ecc_key* private_key, ecc_key* public_key, uint8_t* out, uint32_t* outlen)
{
    int ret = 0;

    pkc_handle_t PKCHandle;
    ecc_curve_init_t ECC_CurveInitStruct = LL_ECC_CURVE_SECP256R1_CONFIG;
    pkc_ecc_point_multi_t PKC_ECCPointMulStruct;
    ecc_point_t Out = {{0}, {0}};
    ecc_point_t In_point = {{0}, {0}};
    uint8_t in_k[32] = {0};

    /* PKC init */
    PKCHandle.p_instance         = PKC;
    PKCHandle.p_result           = &Out;
    PKCHandle.init.p_ecc_curve   = &ECC_CurveInitStruct;
    PKCHandle.init.data_bits   = 256;
    PKCHandle.init.secure_mode = PKC_SECURE_MODE_DISABLE;
    PKCHandle.init.random_func = (uint32_t (*)(void))rand;
    hal_pkc_deinit(&PKCHandle);
    hal_pkc_init(&PKCHandle);

    /* load data into bytes */
    mp_to_unsigned_bin_len(&(private_key->k), in_k, 32);
    mp_to_unsigned_bin_len(public_key->pubkey.x, (uint8_t *)In_point.X, 32);
    mp_to_unsigned_bin_len(public_key->pubkey.y, (uint8_t *)In_point.Y, 32);

    swap_endian((uint32_t *)in_k, 8, (uint32_t *)in_k);
    swap_endian((uint32_t *)In_point.X, 8, (uint32_t *)In_point.X);
    swap_endian((uint32_t *)In_point.Y, 8, (uint32_t *)In_point.Y);

    /* PKC start */
    PKC_ECCPointMulStruct.p_K = (uint32_t *)in_k;
    PKC_ECCPointMulStruct.p_ecc_point = &In_point;

    ret = hal_pkc_ecc_point_multi(&PKCHandle, &PKC_ECCPointMulStruct, 5000);
    if (ret != HAL_OK)
    {
        goto exit;
    }

    swap_endian((uint32_t *)Out.X, 8, (uint32_t *)Out.X);
    swap_endian((uint32_t *)Out.Y, 8, (uint32_t *)Out.Y);

    /* Output X component as shared secret */
    memcpy(out, (uint8_t *)Out.X, 32);
    *outlen = 32;

exit:
    memset(in_k, 0, sizeof(in_k));
    memset((uint8_t *)In_point.X, 0, sizeof(In_point.X));
    memset((uint8_t *)In_point.Y, 0, sizeof(In_point.Y));
    memset((uint8_t *)Out.X, 0, sizeof(Out.X));
    memset((uint8_t *)Out.Y, 0, sizeof(Out.Y));
    hal_pkc_deinit(&PKCHandle);

    return ret;
}


int gr5xxx_ecc_verify_hash(const uint8_t* sig, uint32_t siglen, const uint8_t* hash, uint32_t hashlen, int* res, ecc_key* key)
{
    int ret = 0;
    uint8_t public_key[65] = {0};
    uint32_t public_key_len = 256;
    mp_int r, s;

    algo_ecc_ecdsa_config_t ctx;
    uint32_t r_val[8] = {0};
    uint32_t s_val[8] = {0};

    if (sig == NULL || hash == NULL || res == NULL || key == NULL)
        return HAL_ERROR;

    /* Default to invalid signature */
    *res = 0;

    /* Set ECC Curve */
    crypto_ecc_ecdsa_init(&ctx, ECC_CURVE_SECP256R1);

    /* Decode DSA header */
    mp_init(&r);
    mp_init(&s);
    ret = DecodeECC_DSA_Sig(sig, siglen, &r, &s);
    if (ret != 0)
    {
        return ret;
    }
    mp_to_unsigned_bin_len(&r, (uint8_t *)r_val, 32);
    mp_to_unsigned_bin_len(&s, (uint8_t *)s_val, 32);
    swap_endian((uint32_t *)r_val, 8, (uint32_t *)r_val);
    swap_endian((uint32_t *)s_val, 8, (uint32_t *)s_val);

    /* Export public key */
    ret = wc_ecc_export_x963(key, public_key, &public_key_len);
    if (ret != 0)
    {
        return ret;
    }

    swap_endian((uint32_t *)(public_key + 1), 16, (uint32_t *)(public_key + 1));
    memcpy(&(ctx.our_public_point), public_key + 1, 64);

    /* ECDSA verification process */
    ret = crypto_ecc_ecdsa_verify(&ctx, ECC_HASH_NONE, (uint8_t *)hash, hashlen, r_val, s_val);
    if (ret == 0)
    {
        *res = 1;
    }

    return ret;
}


