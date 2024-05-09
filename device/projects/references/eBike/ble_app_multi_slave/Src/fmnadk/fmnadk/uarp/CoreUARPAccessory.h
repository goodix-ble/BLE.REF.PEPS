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


#ifndef uarpAccessory_h
#define uarpAccessory_h

#include "CoreUARPPlatform.h"
#include "CoreUARPProtocolDefines.h"
#include "CoreUARPUtils.h"

#ifdef __cplusplus
extern "C" {
#endif

/* MARK: Forward Declarations */

struct uarpAccessoryObj;
struct uarpRemoteControllerObj;

/* MARK: Callbacks */


/* -------------------------------------------------------------------------------- */
/*! @brief request buffer for tx'ing a uarp message
    @discussion this routine is called when the lower layer would like a buffer for the purpose of sending a uarp message
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param pControllerDelegate pointer to the controller delegate, passed into this layer's controller add
    @param ppBuffer double pointer where the buffer pointer will be returned. Callback managed.  Caller MUST NOT free this memory
    @param pLength value to return the length of the buffer needed
    @return kUARPStatusXXX
 */
/* -------------------------------------------------------------------------------- */
typedef uint32_t (* fcnUarpAccessoryRequestTransmitMsgBuffer)( void *pAccessoryDelegate, void *pControllerDelegate,
                                                              uint8_t **ppBuffer, uint32_t *pLength );


/* -------------------------------------------------------------------------------- */
/*! @brief return previously allocated buffer
    @discussion this routine is called when the lower layer is done with a dynamically allocated buffer for a uarp message
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param pControllerDelegate pointer to the controller delegate, passed into this layer's controller add
    @param pBuffer pointer to the buffer being returned
 */
/* -------------------------------------------------------------------------------- */
typedef void (* fcnUarpAccessoryReturnTransmitMsgBuffer)( void *pAccessoryDelegate, void *pControllerDelegate,
                                                         uint8_t *pBuffer );


/* -------------------------------------------------------------------------------- */
/*! @brief send uarp message to controller
    @discussion this routine is called when the lower layer needs to send a uarp message to the controller
    @param pAccessoryDelegate pointer to the firmware updater delegate's accessory context pointer
    @param pControllerDelegate pointer to the firmware updater delegate's controller context pointer
    @param pBuffer pointer to the buffer being sent
    @param length length of the buffer pointed to above
    @return kUARPStatusXXX
 */
/* -------------------------------------------------------------------------------- */
typedef uint32_t (* fcnUarpAccessorySendMessage)( void *pAccessoryDelegate, void *pControllerDelegate,
                                                 uint8_t *pBuffer, uint32_t length );

/* -------------------------------------------------------------------------------- */
/*! @brief apply staged assets
    @discussion this routine is called when the lower layer has been instructed to apply staged assets by the controller
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param pControllerDelegate pointer to the controller delegate, passed into this layer's controller add
    @param pFlags pointer to return apply staged assets flags
    @return kUARPStatusXXX
 */
/* -------------------------------------------------------------------------------- */
typedef uint32_t (* fcnUarpAccessoryApplyStagedAssets)( void *pAccessoryDelegate, void *pControllerDelegate,
                                                       uint16_t *pFlags );

/* -------------------------------------------------------------------------------- */
/*! @brief query accessory info
    @discussion this routine is called when the controller has requested one of the info TLVs
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param infoType accessory information type
    @param pBuffer buffer for responding to the accessory information
    @param lengthBuffer length of buffer pointed to above
    @param pLengthNeeded length needed to respond to this query
    @return kUARPStatusXXX
 */
/* -------------------------------------------------------------------------------- */
typedef uint32_t (* fcnUarpAccessoryQueryAccessoryInfo)( void *pAccessoryDelegate, uint32_t infoType, void *pBuffer,
                                                        uint32_t lengthBuffer, uint32_t *pLengthNeeded );

/* -------------------------------------------------------------------------------- */
/*! @brief asset offered
    @discussion this routine is called when the controller has offered an asset
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param pControllerDelegate pointer to the controller delegate, passed into this layer's controller add
    @param pAssetCore pointer to the core asset object
    @return kUARPStatusXXX
 */
/* -------------------------------------------------------------------------------- */
typedef uint32_t (* fcnUarpAccessoryAssetOffered)( void *pAccessoryDelegate, void *pControllerDelegate,
                                                  struct uarpAssetCoreObj *pAssetCore );

/* -------------------------------------------------------------------------------- */
/*! @brief asset rescinded
    @discussion this routine is called when the controller has rescinded a previously offered an asset
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param pControllerDelegate pointer to the controller delegate, passed into this layer's controller add
    @param assetID  pointer to the core asset object
 */
/* -------------------------------------------------------------------------------- */
typedef void (* fcnUarpAccessoryAssetRescinded)( void *pAccessoryDelegate, void *pControllerDelegate, uint16_t assetID );

/* -------------------------------------------------------------------------------- */
/*! @brief data response
    @discussion this routine is called when the controller has responded to a data request
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param pControllerDelegate pointer to the controller delegate, passed into this layer's controller add
    @param assetID asset ID
    @param pBuffer pointer to the buffer containing response bytes
    @param length length of the buffer pointed to above
    @param offset offset into the asset, based on zero
    @return kUARPStatusXXX
 */
/* -------------------------------------------------------------------------------- */
typedef uint32_t (* fcnUarpAccessoryAssetDataResponse)( void *pAccessoryDelegate, void *pControllerDelegate,
                                                       uint16_t assetID,
                                                       uint8_t *pBuffer, uint32_t length, uint32_t offset );

/* -------------------------------------------------------------------------------- */
/*! @brief pause transfers
    @discussion this routine is called when the lower layer has been instructed to pause data requests by the controller
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param pControllerDelegate pointer to the controller delegate, passed into this layer's controller add
    @return kUARPStatusXXX
 */
/* -------------------------------------------------------------------------------- */
typedef uint32_t (* fcnUarpAccessoryUpdateDataTransferPause)( void *pAccessoryDelegate, void *pControllerDelegate );

/* -------------------------------------------------------------------------------- */
/*! @brief resume transfers
    @discussion this routine is called when the lower layer has been instructed to resume previously paused data requesrs by the controller
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param pControllerDelegate pointer to the controller delegate, passed into this layer's controller add
    @return kUARPStatusXXX
 */
/* -------------------------------------------------------------------------------- */
typedef uint32_t (* fcnUarpAccessoryUpdateDataTransferResume)( void *pAccessoryDelegate, void *pControllerDelegate );

/* -------------------------------------------------------------------------------- */
/*! @brief vendor specific callback
    @discussion this routine is called when a vendor specific message has been received
    @param pAccessoryDelegate pointer to the accessory delegate, passed into this layer's accessory init
    @param pControllerDelegate pointer to the controller delegate, passed into this layer's controller add
    @param oui vendor specific OUI
    @param msgType vendor specific message type
    @param pBuffer pointer to the buffer for the vendor specific message
    @param lengthBuffer size of the buffer representing the payload of the vendor specific message
    @return kUARPStatusXXX
 */
/* -------------------------------------------------------------------------------- */
typedef uint32_t (* fcnUarpVendorSpecific)( void *pAccessoryDelegate, void *pControllerDelegate,
                                           uint8_t oui[kUARPOUILength], uint16_t msgType,
                                           uint8_t *pBuffer, uint32_t lengthBuffer );


/* MARK: Objects */


/* UARP Remote Controller Object - callbacks */

struct uarpAccessoryCallbacksObj
{
    fcnUarpAccessoryRequestTransmitMsgBuffer fRequestTransmitMsgBuffer;     /* required */
    fcnUarpAccessoryReturnTransmitMsgBuffer fReturnTransmitMsgBuffer;       /* required */
    fcnUarpAccessorySendMessage fSendMessage;                               /* required */
    fcnUarpAccessoryQueryAccessoryInfo fAccessoryQueryAccessoryInfo;        /* required */
    fcnUarpAccessoryAssetOffered fAccessoryAssetOffered;                    /* required */
    fcnUarpAccessoryAssetRescinded fAssetRescinded;                         /* required */
    fcnUarpAccessoryAssetDataResponse fAccessoryAssetDataResponse;          /* required */
    fcnUarpAccessoryUpdateDataTransferPause fUpdateDataTransferPause;       /* required */
    fcnUarpAccessoryUpdateDataTransferResume fUpdateDataTransferResume;     /* required */
    fcnUarpAccessoryApplyStagedAssets fApplyStagedAssets;                   /* required */
    fcnUarpVendorSpecific fVendorSpecific;                                  /* optional */
};


/* UARP Accessory Object */

struct uarpAccessoryObj
{
    void *pDelegate; /* it is up to the implementer to define this context */

    int nextRemoteControllerID;
    
    struct uarpAccessoryCallbacksObj callbacks;
    
    struct uarpRemoteControllerObj *pControllerList;
};


/* UARP Remote Controller Object - internal usage */

struct uarpRemoteControllerObj
{
    void *pDelegate; /* it is up to the implementer to define this context */

    int remoteControllerID;
    UARPBool dataTransferAllowed;
    
    uint16_t txMsgID;
    uint16_t lastRxMsgID;

    uint32_t selectedProtocolVersion;
    
    struct UARPStatistics uarpStats;
    
    struct uarpRemoteControllerObj *pNext;
};

uint32_t uarpAccessoryInit( struct uarpAccessoryObj *pAccessory,
                           struct uarpAccessoryCallbacksObj *pCallbacks,
                           void *pAccessoryDelegate );

uint32_t uarpAccessoryRemoteControllerAdd( struct uarpAccessoryObj *pAccessory,
                                          struct uarpRemoteControllerObj *pRemoteController,
                                          void *pControllerDelegate );

uint32_t uarpAccessoryRemoteControllerRemove( struct uarpAccessoryObj *pAccessory,
                                             struct uarpRemoteControllerObj *pRemoteController );

uint32_t uarpAccessoryRecvMessage( struct uarpAccessoryObj *pAccessory, void *pControllerDelegate,
                                  uint8_t *pRxMsg, uint32_t lengthRxMsg );

uint32_t uarpAccessoryAssetRequestData( struct uarpAccessoryObj *pAccessory, void *pControllerDelegate,
                                       uint16_t assetID, uint32_t requestOffset, uint32_t requestLength );

uint32_t uarpAccessoryAssetDeny( struct uarpAccessoryObj *pAccessory, void *pControllerDelegate, uint16_t assetID );

uint32_t uarpAccessoryAssetAbandon( struct uarpAccessoryObj *pAccessory, void *pControllerDelegate, uint16_t assetID );

uint32_t uarpAccessoryAssetStaged( struct uarpAccessoryObj *pAccessory, void *pControllerDelegate, uint16_t assetID );

uint32_t uarpAccessoryAssetCorrupt( struct uarpAccessoryObj *pAccessory, void *pControllerDelegate, uint16_t assetID );

#if !(UARP_DISABLE_VENDOR_SPECIFIC)

uint32_t uarpAccessoryTxMsgVendorSpecific( struct uarpAccessoryObj *pAccessory, void *pControllerDelegate,
                                          uint8_t oui[kUARPOUILength], uint16_t msgType,
                                          uint8_t *pBuffer, uint32_t lengthBuffer );

#endif

#ifdef __cplusplus
}
#endif

#endif /* uarpAccessory_h */
