/*
 * Disclaimer: IMPORTANT: This Apple software is supplied to you, by Apple Inc. ("Apple"), in your
 * capacity as a current, and in good standing, Licensee in the MFi Licensing Program. Use of this
 * Apple software is governed by and subject to the terms and conditions of your MFi License,
 * including, but not limited to, the restrictions specified in the provision entitled "Public
 * Software", and is further subject to your agreement to the following additional terms, and your
 * agreement that the use, installation, modification or redistribution of this Apple software
 * constitutes acceptance of these additional terms. If you do not agree with these additional terms,
 * you may not use, install, modify or redistribute this Apple software.
 *
 * Subject to all of these terms and in consideration of your agreement to abide by them, Apple grants
 * you, for as long as you are a current and in good-standing MFi Licensee, a personal, non-exclusive
 * license, under Apple's copyrights in this Apple software (the "Apple Software"), to use,
 * reproduce, and modify the Apple Software in source form, and to use, reproduce, modify, and
 * redistribute the Apple Software, with or without modifications, in binary form, in each of the
 * foregoing cases to the extent necessary to develop and/or manufacture "Proposed Products" and
 * "Licensed Products" in accordance with the terms of your MFi License. While you may not
 * redistribute the Apple Software in source form, should you redistribute the Apple Software in binary
 * form, you must retain this notice and the following text and disclaimers in all such redistributions
 * of the Apple Software. Neither the name, trademarks, service marks, or logos of Apple Inc. may be
 * used to endorse or promote products derived from the Apple Software without specific prior written
 * permission from Apple. Except as expressly stated in this notice, no other rights or licenses,
 * express or implied, are granted by Apple herein, including but not limited to any patent rights that
 * may be infringed by your derivative works or by other works in which the Apple Software may be
 * incorporated. Apple may terminate this license to the Apple Software by removing it from the list
 * of Licensed Technology in the MFi License, or otherwise in accordance with the terms of such MFi License.
 *
 * Unless you explicitly state otherwise, if you provide any ideas, suggestions, recommendations, bug
 * fixes or enhancements to Apple in connection with this software ("Feedback"), you hereby grant to
 * Apple a non-exclusive, fully paid-up, perpetual, irrevocable, worldwide license to make, use,
 * reproduce, incorporate, modify, display, perform, sell, make or have made derivative works of,
 * distribute (directly or indirectly) and sublicense, such Feedback in connection with Apple products
 * and services. Providing this Feedback is voluntary, but if you do provide Feedback to Apple, you
 * acknowledge and agree that Apple may exercise the license granted above without the payment of
 * royalties or further consideration to Participant.
 * The Apple Software is provided by Apple on an "AS IS" basis. APPLE MAKES NO WARRANTIES, EXPRESS OR
 * IMPLIED, INCLUDING WITHOUT LIMITATION THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND OPERATION ALONE OR
 * IN COMBINATION WITH YOUR PRODUCTS.
 *
 * IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION, MODIFICATION
 * AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED AND WHETHER UNDER THEORY OF CONTRACT, TORT
 * (INCLUDING NEGLIGENCE), STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright (C) 2020 Apple Inc. All Rights Reserved.
 */

#define APP_LOG_TAG "[UARP]"

#include "CoreUARPAPPAccessory.h"

#include "fmna_constants_platform.h"

#ifdef UNUSED
#undef  UNUSED
#define UNUSED __attribute__ ((unused))
#endif

#define CRITICAL_REGION_ENTER() GLOBAL_EXCEPTION_DISABLE()
#define CRITICAL_REGION_EXIT()  GLOBAL_EXCEPTION_ENABLE()

static uint8_t                  s_uarp_stage_apply_pending;
static struct UARPAppAccessory *s_accessory_ptr;

/* MARK: INTERNAL PROTOTYPES */

static uint32_t UARPAppRequestBuffer( void *pAccessoryDelegate, uint8_t **ppBuffer, uint32_t bufferLength );

static void UARPAppReturnBuffer( void *pAccessoryDelegate, uint8_t *pBuffer );

static uint32_t UARPAppRequestTransmitMsgBuffer( void *pAccessoryDelegate, void *pControllerDelegate,
                                                  uint8_t **ppBuffer, uint32_t *pLength );

static void UARPAppReturnTransmitMsgBuffer( void *pAccessoryDelegate, void *pControllerDelegate, uint8_t *pBuffer );

static uint32_t UARPAppSendMessage( void *pAccessoryDelegate, void *pControllerDelegate,
                                     uint8_t *pBuffer, uint32_t length );

static uint32_t UARPAppDataTransferPause( void *pAccessoryDelegate, void *pControllerDelegate );

static uint32_t UARPAppDataTransferResume( void *pAccessoryDelegate, void *pControllerDelegate );

static void UARPAppSuperBinaryOffered( void *pAccessoryDelegate, void *pControllerDelegate,
                                        struct uarpPlatformAsset *pAsset );

static void UARPAppDynamicAssetOffered( void *pAccessoryDelegate, void *pControllerDelegate,
                                         struct uarpPlatformAsset *pAsset );

static void UARPAppAssetOrphaned( void *pAccessoryDelegate, void *pAssetDelegate );

static void UARPAppAssetRescinded( void *pAccessoryDelegate, void *pControllerDelegate, void *pAssetDelegate );

static void UARPAppAssetCorrupt( void *pAccessoryDelegate, void *pAssetDelegate );

static void UARPAppAssetReady( void *pAccessoryDelegate, void *pAssetDelegate );

static void UARPAppAssetMetaDataTLV( void *pAccessoryDelegate, void *pAssetDelegate,
                                      uint32_t tlvType, uint32_t tlvLength, uint8_t *pTlvValue );

static void UARPAppAssetMetaDataComplete( void *pAccessoryDelegate, void *pAssetDelegate );

static void UARPAppPayloadReady( void *pAccessoryDelegate, void *pAssetDelegate );

static void UARPAppPayloadMetaDataTLV( void *pAccessoryDelegate, void *pAssetDelegate,
                                        uint32_t tlvType, uint32_t tlvLength, uint8_t *pTlvValue );

static void UARPAppPayloadMetaDataComplete( void *pAccessoryDelegate, void *pAssetDelegate );

static void UARPAppPayloadData( void *pAccessoryDelegate, void *pAssetDelegate,
                                 uint8_t *pBuffer, uint32_t lengthBuffer, uint32_t offset,
                                 uint8_t *pAssetState, uint32_t lengthAssetState );

static void UARPAppPayloadDataComplete( void *pAccessoryDelegate, void *pAssetDelegate );

static uint32_t UARPAppApplyStagedAssets( void *pAccessoryDelegate, void *pControllerDelegate, uint16_t *pFlags );

static uint32_t UARPAppQueryManufacturerName( void *pAccessoryDelegate, uint8_t *pOptionString, uint32_t *pLength );

static uint32_t UARPAppQueryModelName( void *pAccessoryDelegate, uint8_t *pOptionString, uint32_t *pLength );

static uint32_t UARPAppQuerySerialNumber( void *pAccessoryDelegate, uint8_t *pOptionString, uint32_t *pLength );

static uint32_t UARPAppQueryHardwareVersion( void *pAccessoryDelegate, uint8_t *pOptionString, uint32_t *pLength );

static uint32_t UARPAppQueryActiveFirmwareVersion( void *pAccessoryDelegate, uint32_t assetTag,
                                                  struct UARPVersion *pVersion );

static uint32_t UARPAppQueryStagedFirmwareVersion( void *pAccessoryDelegate, uint32_t assetTag,
                                                  struct UARPVersion *pVersion );

static uint32_t UARPAppQueryLastError( void *pAccessoryDelegate, struct UARPLastErrorAction *pLast );

/* MARK: CONTROL */

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppInit( struct UARPAppAccessory *pAccessory,
                       const char *manufacturerName,
                       const char *modelName,
                       const char *serialNumber,
                       const char *hardwareVersion,
                       struct UARPVersion *pActiveFirmwareVersion,
                       struct UARPVersion *pStagedFirmwareVersion,
                       struct UARPAppCallbacks *pCallbacks )
{
    size_t length;
    uint32_t status;
    struct uarpPlatformOptionsObj options;
    struct uarpPlatformAccessoryCallbacks callbacks;

    s_uarp_stage_apply_pending = 0;

    /* initialize and copy inputs */
    memset( pAccessory, 0, sizeof( struct UARPAppAccessory ) );

    length = strlen( manufacturerName );
    __UARP_Require_Action( ( length <= kUARPAppInfoLength ), exit, status = kUARPStatusInvalidArgument );

    memcpy( pAccessory->manufacturerName, manufacturerName, length );

    length = strlen( modelName );
    __UARP_Require_Action( ( length <= kUARPAppInfoLength ), exit, status = kUARPStatusInvalidArgument );

    memcpy( pAccessory->modelName, modelName, length );

    length = strlen( serialNumber );
    __UARP_Require_Action( ( length <= kUARPAppInfoLength ), exit, status = kUARPStatusInvalidArgument );

    memcpy( pAccessory->serialNumber, serialNumber, length );

    length = strlen( hardwareVersion );
    __UARP_Require_Action( ( length <= kUARPAppInfoLength ), exit, status = kUARPStatusInvalidArgument );

    memcpy( pAccessory->hardwareVersion, hardwareVersion, length );

    __UARP_Require_Action( ( pActiveFirmwareVersion != NULL ), exit, status = kUARPStatusInvalidArgument );
    pAccessory->activeFirmwareVersion = *pActiveFirmwareVersion;

    __UARP_Require_Action( ( pStagedFirmwareVersion != NULL ), exit, status = kUARPStatusInvalidArgument );
    pAccessory->stagedFirmwareVersion = *pStagedFirmwareVersion;

    pAccessory->lastAction = kUARPLastActionApplyFirmwareUpdate;
    pAccessory->lastActionStatus = 0x08675309;

    options.maxTxPayloadLength = kUARPAppMaxTxMsgPayloadSize;
    options.maxRxPayloadLength = kUARPAppMaxRxMsgPayloadSize;
    options.payloadWindowLength = kUARPAppPayloadWindowSize;

    pAccessory->callbacks = *pCallbacks;
    __UARP_Require_Action( ( pAccessory->callbacks.fSendMessage != NULL ), exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( ( pAccessory->callbacks.fUarpEventHandler != NULL ), exit, status = kUARPStatusInvalidArgument );

    /* setup our callbacks */
    callbacks.fRequestBuffer = UARPAppRequestBuffer;
    callbacks.fReturnBuffer = UARPAppReturnBuffer;
    callbacks.fRequestTransmitMsgBuffer = UARPAppRequestTransmitMsgBuffer;
    callbacks.fReturnTransmitMsgBuffer = UARPAppReturnTransmitMsgBuffer;
    callbacks.fSendMessage = UARPAppSendMessage;
    callbacks.fDataTransferPause = UARPAppDataTransferPause;
    callbacks.fDataTransferResume = UARPAppDataTransferResume;
    callbacks.fSuperBinaryOffered = UARPAppSuperBinaryOffered;
    callbacks.fDynamicAssetOffered = UARPAppDynamicAssetOffered;
    callbacks.fAssetOrphaned = UARPAppAssetOrphaned;
    callbacks.fAssetRescinded = UARPAppAssetRescinded;
    callbacks.fAssetCorrupt = UARPAppAssetCorrupt;
    callbacks.fAssetReady = UARPAppAssetReady;
    callbacks.fAssetMetaDataTLV = UARPAppAssetMetaDataTLV;
    callbacks.fAssetMetaDataComplete = UARPAppAssetMetaDataComplete;
    callbacks.fPayloadReady = UARPAppPayloadReady;
    callbacks.fPayloadMetaDataTLV = UARPAppPayloadMetaDataTLV;
    callbacks.fPayloadMetaDataComplete = UARPAppPayloadMetaDataComplete;
    callbacks.fPayloadData = UARPAppPayloadData;
    callbacks.fPayloadDataComplete = UARPAppPayloadDataComplete;
    callbacks.fApplyStagedAssets = UARPAppApplyStagedAssets;
    callbacks.fManufacturerName = UARPAppQueryManufacturerName;
    callbacks.fModelName = UARPAppQueryModelName;
    callbacks.fSerialNumber = UARPAppQuerySerialNumber;
    callbacks.fHardwareVersion = UARPAppQueryHardwareVersion;
    callbacks.fActiveFirmwareVersion = UARPAppQueryActiveFirmwareVersion;
    callbacks.fStagedFirmwareVersion = UARPAppQueryStagedFirmwareVersion;
    callbacks.fLastError = UARPAppQueryLastError;

    status = uarpPlatformAccessoryInit( &pAccessory->_accessory,
                                       &options,
                                       &callbacks,
                                       NULL,
                                       NULL,
                                       (void *)pAccessory );
    __UARP_Check( status == kUARPStatusSuccess );

    status = kUARPStatusSuccess;

exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppControllerAdd( struct UARPAppAccessory *pAccessory,
                                struct UARPAppController *pController )
{
    uint32_t status;

    status = uarpPlatformControllerAdd( &(pAccessory->_accessory), &(pController->_controller), (void *)pController );
    __UARP_Require( ( status == kUARPStatusSuccess ), exit );

    /* done */
    status = kUARPStatusSuccess;

exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppControllerRemove( struct UARPAppAccessory *pAccessory,
                                   struct UARPAppController *pController )
{
    uint32_t status;

    /* free and pending TX puffers */
    void *pBuf1 = NULL;
    void *pBuf2 = NULL;
    CRITICAL_REGION_ENTER();
    pBuf1 = pController->pBuffer;
    pController->pBuffer = NULL;
    pBuf2 = pController->pPendingBuffer;
    pController->pPendingBuffer = NULL;
    pController->pendingLength = 0;
    CRITICAL_REGION_EXIT();

    if (pBuf1) {
        uarpFree( pBuf1 );
    }
    if (pBuf2) {
        uarpFree( pBuf2 );
    }

    /* alert the lower layer */
    status = uarpPlatformControllerRemove( &(pAccessory->_accessory), &(pController->_controller) );
    __UARP_Check( status == kUARPStatusSuccess );

    pAccessory->pSuperBinary.pController = NULL;

    /* done */
    status = kUARPStatusSuccess;

//exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppRecvMessage( struct UARPAppAccessory *pAccessory,
                              struct UARPAppController *pController,
                              uint8_t *pBuffer, uint32_t length )
{
    uint32_t status;

    status = uarpPlatformAccessoryRecvMessage( &(pAccessory->_accessory), &(pController->_controller),
                                              pBuffer, length );

    return status;
}


/* MARK: CALLBACKS */

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppRequestBuffer( void *pAccessoryDelegate, uint8_t **ppBuffer, uint32_t bufferLength )
{
    uint32_t status;

    __UARP_Require_Action( ppBuffer, exit, status = kUARPStatusInvalidArgument );

    *ppBuffer = uarpZalloc( bufferLength );
    __UARP_Require_Action( *ppBuffer, exit, status = kUARPStatusNoResources );

    status = kUARPStatusSuccess;

exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

void UARPAppReturnBuffer( void *pAccessoryDelegate, uint8_t *pBuffer )
{
    __UARP_Require( pBuffer, exit );

    uarpFree( pBuffer );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppRequestTransmitMsgBuffer( void *pAccessoryDelegate, void *pControllerDelegate,
                                           uint8_t **ppBuffer, uint32_t *pLength )
{
    uint32_t status;
    struct UARPAppAccessory *pAccessory;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( ppBuffer, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pLength, exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    /* alloc the memory and zero it out */
    *pLength = pAccessory->_accessory._options.maxTxPayloadLength + (uint32_t)sizeof( union UARPMessages );

    *ppBuffer = uarpZalloc( *pLength );
    __UARP_Require_Action( *ppBuffer, exit, status = kUARPStatusNoResources );

    /* done */
    status = kUARPStatusSuccess;

exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

void UARPAppReturnTransmitMsgBuffer( void *pAccessoryDelegate, void *pControllerDelegate, uint8_t *pBuffer )
{
    __UARP_Require( pBuffer, exit );

     uarpFree( pBuffer );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppSendMessage( void *pAccessoryDelegate, void *pControllerDelegate, uint8_t *pBuffer, uint32_t length )
{
    uint32_t status;
    struct UARPAppAccessory *pAccessory;
    UNUSED struct UARPAppController *pController;
    uint8_t * pBuf = NULL;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pControllerDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pBuffer, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( ( length > 0 ), exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    pController = (struct UARPAppController *)pControllerDelegate;

    CRITICAL_REGION_ENTER();
    if (pController->pBuffer == NULL) {
        // Set the buffer to return later
        pController->pBuffer = pBuffer;
        pBuf  = pBuffer;
    }
    else {
        if (pController->pPendingBuffer != NULL) {
            uarpLogError(kUARPLoggingCategoryProduct, "Already have a pending UARP TX");
        }
        pController->pPendingBuffer = pBuffer;
        pController->pendingLength = length;
        status = kUARPStatusSuccess;
    }
    CRITICAL_REGION_EXIT();

    if (pBuf) {
        status = pAccessory->callbacks.fSendMessage( pAccessoryDelegate, pControllerDelegate, pBuffer, length );
    }

exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppSendMessageComplete( void *pAccessoryDelegate, void *pControllerDelegate)
{
     uarpLogInfo(kUARPLoggingCategoryProduct, "Send message complete.");
    uint32_t status;
    struct UARPAppAccessory *pAccessory;
    struct UARPAppController *pController;
    uint8_t * pBuf;
    uint16_t length = 0;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pControllerDelegate, exit, status = kUARPStatusInvalidArgument );


    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    pController = (struct UARPAppController *)pControllerDelegate;

    __UARP_Require_Action( (pController->pBuffer), exit, status = kUARPStatusInvalidArgument );

    /* TODO: This should only called by UARP Layer 3 when the accessory is 100% sure it is done with the buffer;
        uarpPlatformAccessorySendMessageComplete() will free the buffer */
    pBuf = pController->pBuffer;

    CRITICAL_REGION_ENTER();
    // See if there is a pending TX and send if there is.
    pController->pBuffer = NULL;
    if (pController->pPendingBuffer) {
        pController->pBuffer = pController->pPendingBuffer;
        length = pController->pendingLength;
        pController->pPendingBuffer = NULL;
    }
    CRITICAL_REGION_EXIT();

    uarpPlatformAccessorySendMessageComplete( &(pAccessory->_accessory), &(pController->_controller), pBuf );

    // there's another packet queued, so send it
    if (pController->pBuffer) {
        uarpLogInfo(kUARPLoggingCategoryProduct, "Sending queued TX packet");
        pAccessory->callbacks.fSendMessage( pAccessoryDelegate, pControllerDelegate, pController->pBuffer, length );
    }

    /* done */
    status = kUARPStatusSuccess;

exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppDataTransferPause( void *pAccessoryDelegate, void *pControllerDelegate )
{
    uint32_t status;
    UNUSED struct UARPAppAccessory *pAccessory;

    /* alias delegates */
    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    uarpLogInfo( kUARPLoggingCategoryProduct, "Data transfers PAUSED from Remote Controller %u",
                ((struct UARPAppController *)pControllerDelegate)->_controller._controller.remoteControllerID );

    /* done */
    status = kUARPStatusSuccess;

//exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppDataTransferResume( void *pAccessoryDelegate, void *pControllerDelegate )
{
    uint32_t status;
    UNUSED struct UARPAppAccessory *pAccessory;

    /* alias delegates */
    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    uarpLogInfo( kUARPLoggingCategoryProduct, "Data transfers RESUMED from Remote Controller %u",
                ((struct UARPAppController *)pControllerDelegate)->_controller._controller.remoteControllerID );

    /* done */
    status = kUARPStatusSuccess;

//exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

void UARPAppSuperBinaryOffered( void *pAccessoryDelegate, void *pControllerDelegate, struct uarpPlatformAsset *pAsset )
{
    UARPBool isAcceptable;
    uint32_t status;
    UARPVersionComparisonResult compareResult;
    struct UARPAppAccessory *pAccessory;
    struct UARPAppController *pController;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pControllerDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pAsset, exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    pController = (struct UARPAppController *)pControllerDelegate;

    uarpLogDebug( kUARPLoggingCategoryPlatform, "Super binary offered from UARP controller:%d ID:%u",
                 pController->_controller._controller.remoteControllerID,
                 pAsset->core.assetID );
    uarpLogDebug( kUARPLoggingCategoryPlatform, "- Tag:%d", pAsset->core.assetTag );
    uarpLogDebug( kUARPLoggingCategoryPlatform, "- Flags:0x%08x", pAsset->core.assetFlags);
    uarpLogDebug( kUARPLoggingCategoryPlatform, "- Version:%u.%u.%u.%u",
                 pAsset->core.assetVersion.major,
                 pAsset->core.assetVersion.minor,
                 pAsset->core.assetVersion.release,
                 pAsset->core.assetVersion.build );
    uarpLogDebug( kUARPLoggingCategoryPlatform, "- Total length:%u", pAsset->core.assetTotalLength);
    uarpLogDebug( kUARPLoggingCategoryPlatform, "- Payloads num:%u", pAsset->core.assetNumPayloads);

    /* Ensure this is an acceptable offer */
    status = uarpPlatformAccessoryAssetIsAcceptable( &(pAccessory->_accessory), pAsset, &isAcceptable );
    __UARP_Require( ( status == kUARPStatusSuccess ), exit );

    /* If we are acceptable so far, determine our next step */
    if ( isAcceptable == kUARPNo )
    {
        uarpLogInfo( kUARPLoggingCategoryProduct, "Asset is not acceptable" );
    }
    else if ( uarpAssetIsSuperBinary( &(pAsset->core) ) )
    {
        if ( ( pAccessory->pSuperBinary.pAsset != NULL ) && ( pAccessory->pSuperBinary.pController != NULL ) )
        {
            /* TODO: handle competing controllers  */

            isAcceptable = kUARPNo;
        }
        else if ( ( pAccessory->pSuperBinary.pAsset != NULL ) && ( pAccessory->pSuperBinary.pController == NULL ) )
        {
            compareResult = uarpAssetCoreCompare( &(pAccessory->pSuperBinary.pAsset->core),
                                                 &(pAsset->core) );

            if ( compareResult == kUARPVersionComparisonResultIsEqual )
            {
                uarpLogInfo( kUARPLoggingCategoryProduct, "Merging offered SuperBinary and orphaned SuperBinary" );

                status = uarpPlatformAccessorySuperBinaryMerge( &(pAccessory->_accessory),
                                                               pAccessory->pSuperBinary.pAsset,
                                                               pAsset );
                __UARP_Require( ( status == kUARPStatusSuccess ), exit );

                /* slight trick, otherwise we would set the superbinary to the newly orphaned (was merged into existing) */
                pAsset = pAccessory->pSuperBinary.pAsset;

                /* TODO: handle the hasPayloadXXXX */
            }
            else
            {
                uarpPlatformAccessoryAssetAbandon( &(pAccessory->_accessory), NULL, pAccessory->pSuperBinary.pAsset );

                pAccessory->pSuperBinary.pAsset = NULL;
            }
        }
    }

    /* is this asset acceptable or no? handle appropriately */
    if ( isAcceptable == kUARPYes )
    {
        if ( pAccessory->pSuperBinary.pAsset == NULL )
        {
            pAccessory->hasPayload = kUARPNo;
        }

        pAccessory->pSuperBinary.pAsset = pAsset;
        pAccessory->pSuperBinary.pController = pController;

        pAsset->pDelegate = &(pAccessory->pSuperBinary);

        uarpLogInfo(kUARPLoggingCategoryProduct, "Super binary accept.");
        status = uarpPlatformAccessoryAssetAccept( &(pAccessory->_accessory), &(pController->_controller), pAsset );
        __UARP_Require( ( status == kUARPStatusSuccess ), exit );
    }
    else
    {
        uarpLogInfo(kUARPLoggingCategoryProduct, "Super binary deny.");
        status = uarpPlatformAccessoryAssetDeny( &(pAccessory->_accessory), &(pController->_controller), pAsset );
        __UARP_Require( ( status == kUARPStatusSuccess ), exit );
    }

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppDynamicAssetOffered( void *pAccessoryDelegate, void *pControllerDelegate,
                                  struct uarpPlatformAsset *pAsset )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Dynamic asset offered.");
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppAssetRescinded( void *pAccessoryDelegate, void *pControllerDelegate, void *pAssetDelegate )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Asset rescinded.");
    struct UARPAppAsset *pAsset;
    struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    uarpLogInfo( kUARPLoggingCategoryPlatform, "Asset %u Rescinded from Controller %d",
                pAsset->pAsset->core.assetID,
                pAsset->pController->_controller._controller.remoteControllerID );

    if ( pAsset == &(pAccessory->pSuperBinary) )
    {
        pAccessory->pSuperBinary.pController = NULL;
    }

    /* must unstage asset */
    memset( &pAccessory->stagedFirmwareVersion, 0, sizeof( pAccessory->stagedFirmwareVersion ) );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppAssetCorrupt( void *pAccessoryDelegate, void *pAssetDelegate )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Asset corrupt.");
    struct UARPAppAsset *pAsset;
    struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    uarpLogInfo( kUARPLoggingCategoryPlatform, "Asset %u Corrupt from Controller %d",
                pAsset->pAsset->core.assetID,
                pAsset->pController->_controller._controller.remoteControllerID );

    if ( pAsset == &(pAccessory->pSuperBinary) )
    {
        pAccessory->pSuperBinary.pController = NULL;
    }

    /* must unstage asset */
    memset( &pAccessory->stagedFirmwareVersion, 0, sizeof( pAccessory->stagedFirmwareVersion ) );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppAssetOrphaned( void *pAccessoryDelegate, void *pAssetDelegate )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Asset orphaned.");
    struct UARPAppAsset *pAsset;
    struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    if ( pAsset == &(pAccessory->pSuperBinary) )
    {
        pAccessory->pSuperBinary.pController = NULL;
    }

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppAssetReady( void *pAccessoryDelegate, void *pAssetDelegate )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Asset Ready.");
    uint32_t status;
    struct UARPAppAsset *pAsset;
    struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    status = uarpPlatformAccessoryAssetRequestMetaData( &(pAccessory->_accessory), pAsset->pAsset );

    if ( status == kUARPStatusNoMetaData )
    {
        uarpLogInfo(kUARPLoggingCategoryProduct, "Asset has not metadata.");
        UARPAppAssetMetaDataComplete( pAccessory, pAsset );
        status = kUARPStatusSuccess;
    }
    __UARP_Require( ( status == kUARPStatusSuccess ), exit );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppAssetMetaDataTLV( void *pAccessoryDelegate, void *pAssetDelegate,
                               uint32_t tlvType, uint32_t tlvLength, uint8_t *pTlvValue )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Asset meta data TLV.");
    switch ( tlvType )
    {
        default:
            uarpLogInfo( kUARPLoggingCategoryProduct, "SuperBinary MetaData Option UNKNOWN %d, length %u",
                        tlvType, tlvLength );
            break;
    }

//exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppAssetMetaDataComplete( void *pAccessoryDelegate, void *pAssetDelegate )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Asset metadata complete.");
    uint32_t status;
    struct UARPAppAsset *pAsset;
    struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    status = uarpPlatformAssetSetPayloadIndex( &(pAccessory->_accessory), pAsset->pAsset,
                                              kUARPAppSuperBinaryPayloadIndexProto );
    __UARP_Require( ( status == kUARPStatusSuccess ), exit );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppPayloadReady( void *pAccessoryDelegate, void *pAssetDelegate )
{
    uint32_t status;
    struct UARPAppAsset *pAsset;
    struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    uarpLogInfo( kUARPLoggingCategoryProduct, "Payload ready - index:%d 4cc:%c%c%c%c",
                pAsset->pAsset->selectedPayloadIndex,
                pAsset->pAsset->payload.payload4cc[0],
                pAsset->pAsset->payload.payload4cc[1],
                pAsset->pAsset->payload.payload4cc[2],
                pAsset->pAsset->payload.payload4cc[3] );

    if (pAccessory->callbacks.fUarpEventHandler)
    {
        UARPAppEvent_t  uarpEvt = {0};

        uarpEvt.evt_id               = kUARPAppEventBinarySize;
        uarpEvt.param.BinaryFileSize = pAsset->pAsset->payload.plHdr.payloadLength;
        pAccessory->callbacks.fUarpEventHandler(&uarpEvt);
    }

    status = uarpPlatformAccessoryPayloadRequestMetaData( &(pAccessory->_accessory), pAsset->pAsset );

    if ( status == kUARPStatusNoMetaData )
    {
        uarpLogInfo(kUARPLoggingCategoryProduct, "Payload has not metadata.");
        UARPAppPayloadMetaDataComplete( pAccessory, pAsset );
        status = kUARPStatusSuccess;
    }
    __UARP_Require( ( status == kUARPStatusSuccess ), exit );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppPayloadMetaDataTLV( void *pAccessoryDelegate, void *pAssetDelegate,
                                 uint32_t tlvType, uint32_t tlvLength, uint8_t *pTlvValue )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Payload metadata TLV.");
    uint32_t status;
    UNUSED struct UARPAppAsset *pAsset;
    UNUSED struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    switch ( tlvType )
    {
        default:

            status = kUARPStatusInvalidTLV;

            break;
    }
    __UARP_Require_Quiet( ( status == kUARPStatusInvalidTLV ), exit );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppPayloadMetaDataComplete( void *pAccessoryDelegate, void *pAssetDelegate )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Payload metadata complete.");
    uint32_t status;
    struct UARPAppAsset *pAsset;
    struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    status = uarpPlatformAccessoryPayloadRequestData( &(pAccessory->_accessory), pAsset->pAsset );
    __UARP_Require( ( status == kUARPStatusSuccess ), exit );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppPayloadData( void *pAccessoryDelegate, void *pAssetDelegate,
                          uint8_t *pBuffer, uint32_t lengthBuffer, uint32_t offset,
                          uint8_t *pAssetState, uint32_t lengthAssetState )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Payload data...");
    uint8_t tagUnknown[kUARPSuperBinaryPayloadTagLength];

    struct UARPAppAsset *pAsset;
    UNUSED struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    if (pAccessory->callbacks.fUarpEventHandler)
    {
        UARPAppEvent_t  uarpEvt = {0};

        uarpEvt.evt_id                      = kUARPAppEventBinaryFragment;
        uarpEvt.param.BinaryFragment.length = lengthBuffer;
        uarpEvt.param.BinaryFragment.p_data = pBuffer;
        uarpEvt.param.BinaryFragment.offset = offset;
        pAccessory->callbacks.fUarpEventHandler(&uarpEvt);
    }

    if ( pAsset->pAsset->payload.plHdr.payloadTag == uarpPayloadTagPack( g_fmna_uarp_cfg._4cc_tag ) )
    {
        uarpLogInfo( kUARPLoggingCategoryProduct, "Payload data rx %u bytes from offset %u", lengthBuffer, offset );
    }
    else
    {
        uarpPayloadTagUnpack( pAsset->pAsset->payload.plHdr.payloadTag, tagUnknown );

        uarpLogInfo( kUARPLoggingCategoryProduct, "Unknown <%c%c%c%c> RX %u bytes from offset %u",
                    tagUnknown[0], tagUnknown[1], tagUnknown[2], tagUnknown[3], lengthBuffer, offset );
    }

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

void UARPAppPayloadDataComplete( void *pAccessoryDelegate, void *pAssetDelegate )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Payload data complete.");
    uint32_t status;
    struct UARPAppAsset *pAsset;
    struct UARPAppAccessory *pAccessory;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require( pAccessoryDelegate, exit );

    pAsset = (struct UARPAppAsset *)pAssetDelegate;
    __UARP_Require( pAssetDelegate, exit );

    pAccessory->hasPayload = kUARPYes;

    pAccessory->stagedFirmwareVersion = pAccessory->pSuperBinary.pAsset->core.assetVersion;

    if (pAccessory->callbacks.fUarpEventHandler)
    {
        UARPAppEvent_t  uarpEvt = {0};

        uarpEvt.evt_id  = kUARPAppEventBinaryComplete;
        pAccessory->callbacks.fUarpEventHandler(&uarpEvt);
    }

    status = uarpPlatformAccessoryAssetFullyStaged( &(pAccessory->_accessory), pAsset->pAsset );
    __UARP_Require( ( status == kUARPStatusSuccess ), exit );

exit:
    return;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppApplyStagedAssets( void *pAccessoryDelegate, void *pControllerDelegate, uint16_t *pFlags )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Apply staged asset.");
    uint32_t status;
    struct UARPAppAccessory *pAccessory;
    struct UARPAppController *pController;

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;
    __UARP_Require_Action( pAccessory, exit, status = kUARPStatusInvalidArgument );

    pController = (struct UARPAppController *)pControllerDelegate;
    __UARP_Require_Action( pController, exit, status = kUARPStatusInvalidArgument );

    if ( pAccessory->pSuperBinary.pAsset == NULL )
    {
        uarpLogInfo( kUARPLoggingCategoryProduct, "Apply Staged Assets: Nothing staged" );

        *pFlags = kUARPApplyStagedAssetsFlagsNothingStaged;
    }
    else if ( ( pAccessory->stagedFirmwareVersion.major == 0 ) &&
             ( pAccessory->stagedFirmwareVersion.minor == 0 ) &&
             ( pAccessory->stagedFirmwareVersion.release == 0 ) &&
             ( pAccessory->stagedFirmwareVersion.build == 0 ) )
    {
        uarpLogInfo( kUARPLoggingCategoryProduct, "Apply Staged Assets: Staging SuperBinary" );

        *pFlags = kUARPApplyStagedAssetsFlagsMidUpload;
    }
    else
    {
        uarpLogInfo( kUARPLoggingCategoryProduct, "Apply Staged Assets: Updating Active FW Version to Staged FW Version" );

        pAccessory->activeFirmwareVersion = pAccessory->stagedFirmwareVersion;

        memset( &pAccessory->stagedFirmwareVersion, 0, sizeof( pAccessory->stagedFirmwareVersion ) );

        /* Clean up superbinary */
        uarpPlatformAccessoryAssetRelease( &(pAccessory->_accessory), NULL, pAccessory->pSuperBinary.pAsset );

        pAccessory->pSuperBinary.pAsset = NULL;
        pAccessory->pSuperBinary.pController = NULL;

        pAccessory->hasPayload = kUARPNo;

        /* set flags */
        *pFlags = kUARPApplyStagedAssetsFlagsSuccess;

        uarpPlatformCleanupAssets( &(pAccessory->_accessory) );

        uarpLogInfo( kUARPLoggingCategoryProduct, "Apply Staged Assets pending");
        s_uarp_stage_apply_pending = 1;
        s_accessory_ptr = pAccessory;
    }

    /* done */
    status = kUARPStatusSuccess;

exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

void UARPAppApplyStagedAssetsPendingHandle( void )
{
    if (s_uarp_stage_apply_pending)
    {
        UARPAppEvent_t  uarpEvt = {0};

        uarpEvt.evt_id  = kUARPAppEventBinaryApply;

        s_accessory_ptr->callbacks.fUarpEventHandler(&uarpEvt);
    }
}

uint32_t UARPAppQueryManufacturerName( void *pAccessoryDelegate, uint8_t *pOptionString, uint32_t *pLength )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Query manufacturer name.");
    uint32_t status;
    uint32_t lengthNeeded;
    struct UARPAppAccessory *pAccessory;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pOptionString, exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    lengthNeeded = (uint32_t)strlen( pAccessory->manufacturerName );
    __UARP_Require_Action( ( *pLength >= lengthNeeded ), exit, status = kUARPStatusInvalidLength );

    *pLength = lengthNeeded;

    memcpy( pOptionString, pAccessory->manufacturerName, *pLength );

    status = kUARPStatusSuccess;
    uarpLogInfo(kUARPLoggingCategoryPlatform, "Manufacturer name:%s",
        pAccessory->manufacturerName);
exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppQueryModelName( void *pAccessoryDelegate, uint8_t *pOptionString, uint32_t *pLength )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Query model name.");
    uint32_t status;
    uint32_t lengthNeeded;
    struct UARPAppAccessory *pAccessory;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pOptionString, exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    lengthNeeded = (uint32_t)strlen( pAccessory->modelName );
    __UARP_Require_Action( ( *pLength >= lengthNeeded ), exit, status = kUARPStatusInvalidLength );

    *pLength = lengthNeeded;

    memcpy( pOptionString, pAccessory->modelName, *pLength );

    status = kUARPStatusSuccess;
    uarpLogInfo(kUARPLoggingCategoryPlatform, "Model name:%s",
        pAccessory->modelName);
exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppQuerySerialNumber( void *pAccessoryDelegate, uint8_t *pOptionString, uint32_t *pLength )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Query serial number.");
    uint32_t status;
    uint32_t lengthNeeded;
    struct UARPAppAccessory *pAccessory;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pOptionString, exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    lengthNeeded = (uint32_t)strlen( pAccessory->serialNumber );
    __UARP_Require_Action( ( *pLength >= lengthNeeded ), exit, status = kUARPStatusInvalidLength );

    *pLength = lengthNeeded;

    memcpy( pOptionString, pAccessory->serialNumber, *pLength );

    status = kUARPStatusSuccess;

    uarpLogInfo(kUARPLoggingCategoryPlatform, "Serial number:%s",
        pAccessory->serialNumber);
exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppQueryHardwareVersion( void *pAccessoryDelegate, uint8_t *pOptionString, uint32_t *pLength )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Query hardware version.");
    uint32_t status;
    uint32_t lengthNeeded;
    struct UARPAppAccessory *pAccessory;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pOptionString, exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    lengthNeeded = (uint32_t)strlen( pAccessory->hardwareVersion );
    __UARP_Require_Action( ( *pLength >= lengthNeeded ), exit, status = kUARPStatusInvalidLength );

    *pLength = lengthNeeded;

    memcpy( pOptionString, pAccessory->hardwareVersion, *pLength );

    status = kUARPStatusSuccess;

    uarpLogInfo(kUARPLoggingCategoryPlatform, "Hardware version:%s",
            pAccessory->hardwareVersion);
exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppQueryActiveFirmwareVersion( void *pAccessoryDelegate, uint32_t assetTag, struct UARPVersion *pVersion )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Query active firmware version.");
    uint32_t status;
    struct UARPAppAccessory *pAccessory;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pVersion, exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    if ( assetTag == 0 )
    {
        *pVersion = pAccessory->activeFirmwareVersion;
        status = kUARPStatusSuccess;
    }
    else
    {
        memset( pVersion, 0, sizeof( struct UARPVersion ) );
        status = kUARPStatusInvalidAssetTag;
    }

    uarpLogInfo(kUARPLoggingCategoryPlatform, "Active firmware version:%d.%d.%d.%d",
            pAccessory->activeFirmwareVersion.major,
            pAccessory->activeFirmwareVersion.minor,
            pAccessory->activeFirmwareVersion.release,
            pAccessory->activeFirmwareVersion.build);

exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppQueryStagedFirmwareVersion( void *pAccessoryDelegate, uint32_t assetTag, struct UARPVersion *pVersion )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Query staged firmware version.");
    uint32_t status;
    struct UARPAppAccessory *pAccessory;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pVersion, exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    if ( assetTag == 0 )
    {
        *pVersion = pAccessory->stagedFirmwareVersion;
        status = kUARPStatusSuccess;
    }
    else
    {
        memset( pVersion, 0, sizeof( struct UARPVersion ) );
        status = kUARPStatusInvalidAssetTag;
    }

    uarpLogInfo(kUARPLoggingCategoryPlatform, "Staged firmware version:%d.%d.%d.%d",
            pAccessory->stagedFirmwareVersion.major,
            pAccessory->stagedFirmwareVersion.minor,
            pAccessory->stagedFirmwareVersion.release,
            pAccessory->stagedFirmwareVersion.build);

exit:
    return status;
}

/* -------------------------------------------------------------------------------- */

uint32_t UARPAppQueryLastError( void *pAccessoryDelegate, struct UARPLastErrorAction *pLast )
{
    uarpLogInfo(kUARPLoggingCategoryProduct, "Query laste error.");
    uint32_t status;
    struct UARPAppAccessory *pAccessory;

    __UARP_Require_Action( pAccessoryDelegate, exit, status = kUARPStatusInvalidArgument );
    __UARP_Require_Action( pLast, exit, status = kUARPStatusInvalidArgument );

    pAccessory = (struct UARPAppAccessory *)pAccessoryDelegate;

    pLast->lastAction = pAccessory->lastAction;
    pLast->lastError = pAccessory->lastActionStatus;

    status = kUARPStatusSuccess;

    uarpLogInfo(kUARPLoggingCategoryPlatform, "Last action:%u, last error:%u",
            pAccessory->lastAction,
            pAccessory->lastActionStatus);

exit:
    return status;
}





