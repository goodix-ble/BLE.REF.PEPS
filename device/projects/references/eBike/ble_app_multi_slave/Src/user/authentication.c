#include "authentication.h"
#include "grx_hal.h"

#include "mbedtls/entropy.h"
#include "mbedtls/ctr_drbg.h"
#include "mbedtls/platform.h"
#include "mbedtls/ecdsa.h"
#include "mbedtls/ecdh.h"
#include "mbedtls/cipher.h"
#include "mbedtls/aes.h"
#include "mbedtls/gcm.h"


//1. 第一步
static int entropy_source(void *data, uint8_t *output, size_t len, size_t *olen)
{
    uint32_t rng32;
    rng_handle_t g_rng_handle;

    g_rng_handle.p_instance = RNG;
    g_rng_handle.init.seed_mode  = RNG_SEED_FR0_S0;
    g_rng_handle.init.lfsr_mode  = RNG_LFSR_MODE_59BIT;
    g_rng_handle.init.out_mode   = RNG_OUTPUT_FR0_S0;
    g_rng_handle.init.post_mode  = RNG_POST_PRO_NOT;
    hal_rng_deinit(&g_rng_handle);
    hal_rng_init(&g_rng_handle);

    hal_rng_generate_random_number(&g_rng_handle, NULL, &rng32);

    if (len > sizeof(rng32))
    {
        len = sizeof(rng32);
    }
    memcpy(output, &rng32, len);
    *olen = len;

    hal_rng_deinit(&g_rng_handle);
    return 0;
}

void auth_generate_key(void)
{
    int ret;
    const char *pers = "ecdh_test";
    mbedtls_entropy_context entropy;
    mbedtls_ctr_drbg_context ctr_drbg;
    mbedtls_ecp_point client_pub;
    mbedtls_ecp_group grp;
    mbedtls_mpi client_secret;
    mbedtls_mpi client_pri;
    
    /* 1. init structure */
    mbedtls_entropy_init(&entropy);
    mbedtls_ctr_drbg_init(&ctr_drbg);
    mbedtls_mpi_init(&client_secret);
    mbedtls_mpi_init(&client_pri);
    mbedtls_ecp_group_init(&grp);
    mbedtls_ecp_point_init(&client_pub);

    /* 2. update seed with we own interface ported */
    printf( "\r\nSeeding the random number generator..." );

    mbedtls_entropy_add_source(&entropy, entropy_source, NULL,
                               MBEDTLS_ENTROPY_MAX_GATHER,
                               MBEDTLS_ENTROPY_SOURCE_STRONG);
                               
    ret = mbedtls_ctr_drbg_seed( &ctr_drbg, mbedtls_entropy_func, &entropy,
                               (const unsigned char *) pers,
                               strlen(pers));
    if(ret != 0)
    {
        printf( "failed! mbedtls_ctr_drbg_seed returned %d(-0x%04x)\r\n", ret, -ret);
    }
    /* 3. select ecp group SECP256R1 */
    printf("\r\nSelect ecp group SECP256R1...");

    ret = mbedtls_ecp_group_load(&grp, MBEDTLS_ECP_DP_CURVE25519);
    if(ret != 0)
    {
        printf( "failed! mbedtls_ecp_group_load returned %d(-0x%04x)\r\n", ret, -ret);
    }
    printf("ok\r\n");

    /* 4. Client generate public parameter */
    printf("\r\nClient Generate public parameter...");

    ret = mbedtls_ecdh_gen_public(&grp, &client_pri, &client_pub, mbedtls_ctr_drbg_random, &ctr_drbg);
    if(ret != 0)
    {
        printf( "failed! mbedtls_ecdh_gen_public returned %d(-0x%04x)\r\n", ret, -ret);
    }
    printf( " ok\r\n" );
    unsigned char secret_cli[32] = { 0 };
    mbedtls_ecp_point_read_binary(&grp, &client_pub, secret_cli, 32);
    
    for(uint8_t i=0; i<32; i++)
    {
        printf("%d ,", secret_cli[i]);
    }
}

static mbedtls_ecdh_context ctx_cli;
static mbedtls_entropy_context entropy;
static mbedtls_ctr_drbg_context ctr_drbg;

#define CBC_PADDING_NONE
#define KEY_SIZE_256
#define GCM_TAG_LEN 16
void aes_decode(uint8_t *p_key, uint8_t *p_ecode, uint8_t ecode_len, uint8_t *p_out, uint16_t *p_out_len)
{
    int ret = -1;
    int i;
    size_t len;
    int olen = 0;
    mbedtls_cipher_context_t ctx;
    const mbedtls_cipher_info_t *info;
    /* 1. init cipher structuer */
    mbedtls_cipher_init(&ctx);
    
    /* 2. get info structuer from type */
    info = mbedtls_cipher_info_from_type(MBEDTLS_CIPHER_AES_256_ECB);
    
    /* 3. setup cipher structuer */
    ret = mbedtls_cipher_setup(&ctx, info);
    if (ret != 0) {
        printf("mbedtls_cipher_setup error\r\n");
        goto exit;
    }
    
    /* 4. set padding mode */
    mbedtls_cipher_set_padding_mode( &ctx, MBEDTLS_PADDING_NONE );
    
    /* 5. set key */
    ret = mbedtls_cipher_setkey(&ctx, p_key, 256, MBEDTLS_DECRYPT);
    if (ret != 0) {
        printf("mbedtls_cipher_setkey error %d\r\n", ret);
        goto exit;
    }
    /* 6. update cipher */
    ret = mbedtls_cipher_update(&ctx, p_ecode, ecode_len, p_out, &len);
    if (ret != 0) {
        printf("mbedtls_cipher_update error %d\r\n", ret);
        goto exit;
    }
    olen += len;
    
    printf("p_out len %d\r\n", len);
    /* 7. finish cipher */
    ret = mbedtls_cipher_finish(&ctx, p_out+olen, &len);
    if (ret != 0) {
        goto exit;
    }
    olen += len;
    *p_out_len = olen;
     /* show */
    printf("cipher name:%s block size is:%d\r\n", mbedtls_cipher_get_name(&ctx), mbedtls_cipher_get_block_size(&ctx));
    printf("olen = %d\r\n", olen);
    for(i=0;i<olen;i++)
    {
     printf("%02X ",p_out[i]);
    }
    printf("\r\n");
    
    exit:
    /* 8. free cipher structure */
    mbedtls_cipher_free(&ctx);
}

void curve_init(void)
{
    int ret = 1;
    const char pers[] = "ecdh";
    mbedtls_ecdh_init(&ctx_cli);
    mbedtls_ctr_drbg_init(&ctr_drbg);
    
    mbedtls_entropy_init(&entropy);
    mbedtls_entropy_add_source(&entropy, entropy_source, NULL,
                               MBEDTLS_ENTROPY_MAX_GATHER,
                               MBEDTLS_ENTROPY_SOURCE_STRONG);
                               
  
    if ((ret = mbedtls_ctr_drbg_seed(&ctr_drbg, mbedtls_entropy_func,
                                     &entropy,
                                     (const unsigned char *) pers,
                                     sizeof(pers))) != 0) {
        printf(" failed\n  ! mbedtls_ctr_drbg_seed returned %d\n",
                       ret);
    }
    printf(" ok\n");
}


void curve_25519_gen_public_key(uint8_t *p_cli_to_srv_key)
{
    int ret = 1;
    size_t cli_olen;
    unsigned char cli_to_srv[36];
    /*
     * Client: initialize context and generate keypair
     */
    printf("  . Set up client context, generate EC key pair...");

    ret = mbedtls_ecdh_setup(&ctx_cli, MBEDTLS_ECP_DP_CURVE25519);
    if (ret != 0) {
        printf(" failed\n  ! mbedtls_ecdh_setup returned %d\n", ret);
    }

    ret = mbedtls_ecdh_make_params(&ctx_cli, &cli_olen, cli_to_srv,
                                   sizeof(cli_to_srv),
                                   mbedtls_ctr_drbg_random, &ctr_drbg);
    if (ret != 0) {
        printf(" failed\n  ! mbedtls_ecdh_make_params returned %d\n",
                       ret);
    }
    memcpy(p_cli_to_srv_key, &cli_to_srv[4], 32);
}

void cal_share_key(uint8_t *p_serve_key, uint8_t *secret_cli)
{
    int ret = 1;
    size_t cli_olen;
    uint8_t srv_to_cli[33];
    srv_to_cli[0] = 32;
    memcpy(&srv_to_cli[1], p_serve_key, 32);
    ret = mbedtls_ecdh_read_public(&ctx_cli, srv_to_cli,
                                   sizeof(srv_to_cli));
    if (ret != 0) {
        printf(" failed\n  ! mbedtls_ecdh_read_public returned %d\n",
                       ret);
    }

    printf(" ok\n");

    /*
     * Calculate secrets
     */
    printf("  . Calculate secrets...");
    ret = mbedtls_ecdh_calc_secret(&ctx_cli, &cli_olen, secret_cli,
                                   32,
                                   mbedtls_ctr_drbg_random, &ctr_drbg);
    if (ret != 0) {
        printf(" failed\n  ! mbedtls_ecdh_calc_secret returned %d\n",
                       ret);

    }
    printf(" ok %d\n", cli_olen);
}


void curve_25519_test(void)
{
    int ret = 1;
    int exit_code = MBEDTLS_EXIT_FAILURE;
    mbedtls_ecdh_context ctx_cli, ctx_srv;
    mbedtls_entropy_context entropy;
    mbedtls_ctr_drbg_context ctr_drbg;
    unsigned char cli_to_srv[36], srv_to_cli[33];
    const char pers[] = "ecdh";
    
    size_t srv_olen;
    size_t cli_olen;
    unsigned char secret_cli[32] = { 0 };
    unsigned char secret_srv[32] = { 0 };
    const unsigned char *p_cli_to_srv = cli_to_srv;

    mbedtls_ecdh_init(&ctx_cli);
    mbedtls_ecdh_init(&ctx_srv);
    mbedtls_ctr_drbg_init(&ctr_drbg);
    
    /*
     * Initialize random number generation
     */
    mbedtls_printf("  . Seed the random number generator...");
    fflush(stdout);

    mbedtls_entropy_init(&entropy);
    mbedtls_entropy_add_source(&entropy, entropy_source, NULL,
                               MBEDTLS_ENTROPY_MAX_GATHER,
                               MBEDTLS_ENTROPY_SOURCE_STRONG);
                               
  
    if ((ret = mbedtls_ctr_drbg_seed(&ctr_drbg, mbedtls_entropy_func,
                                     &entropy,
                                     (const unsigned char *) pers,
                                     sizeof(pers))) != 0) {
        printf(" failed\n  ! mbedtls_ctr_drbg_seed returned %d\n",
                       ret);
        goto exit;
    }
    mbedtls_printf(" ok\n");

    /*
     * Client: initialize context and generate keypair
     */
    mbedtls_printf("  . Set up client context, generate EC key pair...");
    fflush(stdout);

    ret = mbedtls_ecdh_setup(&ctx_cli, MBEDTLS_ECP_DP_CURVE25519);
    if (ret != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ecdh_setup returned %d\n", ret);
        goto exit;
    }

    ret = mbedtls_ecdh_make_params(&ctx_cli, &cli_olen, cli_to_srv,
                                   sizeof(cli_to_srv),
                                   mbedtls_ctr_drbg_random, &ctr_drbg);
    if (ret != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ecdh_make_params returned %d\n",
                       ret);
        goto exit;
    }
    
    for(uint8_t i=0; i<36; i++)
    {
        printf("%d ,", cli_to_srv[i]);
    }

    mbedtls_printf(" ok %d\n", cli_olen);
    
     /*
     * Server: initialize context and generate keypair
     */
    mbedtls_printf("  . Server: read params, generate public key...");
    fflush(stdout);

    ret = mbedtls_ecdh_read_params(&ctx_srv, &p_cli_to_srv,
                                   p_cli_to_srv + sizeof(cli_to_srv));
    if (ret != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ecdh_read_params returned %d\n",
                       ret);
        goto exit;
    }

    ret = mbedtls_ecdh_make_public(&ctx_srv, &srv_olen, srv_to_cli,
                                   sizeof(srv_to_cli),
                                   mbedtls_ctr_drbg_random, &ctr_drbg);
    if (ret != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ecdh_make_public returned %d\n",
                       ret);
        goto exit;
    }

     for(uint8_t i=0; i<33; i++)
    {
        printf("%d ,", srv_to_cli[i]);
    }
    mbedtls_printf(" ok %d\n", srv_olen);
    
    /*
     * Client: read public key
     */
    mbedtls_printf("  . Client: read public key...");
    fflush(stdout);

    ret = mbedtls_ecdh_read_public(&ctx_cli, srv_to_cli,
                                   sizeof(srv_to_cli));
    if (ret != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ecdh_read_public returned %d\n",
                       ret);
        goto exit;
    }

    mbedtls_printf(" ok\n");

    /*
     * Calculate secrets
     */
    mbedtls_printf("  . Calculate secrets...");
    fflush(stdout);

    ret = mbedtls_ecdh_calc_secret(&ctx_cli, &cli_olen, secret_cli,
                                   sizeof(secret_cli),
                                   mbedtls_ctr_drbg_random, &ctr_drbg);
    if (ret != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ecdh_calc_secret returned %d\n",
                       ret);
        goto exit;
    }
    mbedtls_printf(" ok %d\n", cli_olen);

    ret = mbedtls_ecdh_calc_secret(&ctx_srv, &srv_olen, secret_srv,
                                   sizeof(secret_srv),
                                   mbedtls_ctr_drbg_random, &ctr_drbg);
    if (ret != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ecdh_calc_secret returned %d\n",
                       ret);
        goto exit;
    }

    mbedtls_printf(" ok %d\n", srv_olen);

    /*
     * Verification: are the computed secrets equal?
     */
    mbedtls_printf("  . Check if both calculated secrets are equal...");
    fflush(stdout);

    ret = memcmp(secret_srv, secret_cli, srv_olen);
    if (ret != 0 || (cli_olen != srv_olen)) {
        mbedtls_printf(" failed\n  ! Shared secrets not equal.\n");
        goto exit;
    }
    
    for(uint8_t i=0; i<32; i++)
    {
        printf("%d ,", secret_srv[i]);
    }
    mbedtls_printf(" ok\n");
    for(uint8_t i=0; i<32; i++)
    {
        printf("%d ,", secret_cli[i]);
    }

    mbedtls_printf(" ok\n");

    exit_code = MBEDTLS_EXIT_SUCCESS;

exit:
    mbedtls_ecdh_free(&ctx_srv);
    mbedtls_ecdh_free(&ctx_cli);
    mbedtls_ctr_drbg_free(&ctr_drbg);
    mbedtls_entropy_free(&entropy);

    //mbedtls_exit(exit_code);
    return;
}


