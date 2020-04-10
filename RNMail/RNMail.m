#import <MessageUI/MessageUI.h>
#import "RNMail.h"
#import <React/RCTConvert.h>
#import <React/RCTLog.h>

@implementation RNMail
{
    NSMutableDictionary *_callbacks;
}

- (instancetype)init
{
    if ((self = [super init])) {
        _callbacks = [[NSMutableDictionary alloc] init];
    }
    return self;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(mail:(NSDictionary *)options
                  callback: (RCTResponseSenderBlock)callback)
{
    if ([MFMailComposeViewController canSendMail])
    {
        MFMailComposeViewController *mail = [[MFMailComposeViewController alloc] init];
        mail.mailComposeDelegate = self;
        _callbacks[RCTKeyForInstance(mail)] = callback;

        if (options[@"subject"]){
            NSString *subject = [RCTConvert NSString:options[@"subject"]];
            [mail setSubject:subject];
        }

        bool *isHTML = NO;

        if (options[@"isHTML"]){
            isHTML = [options[@"isHTML"] boolValue];
        }

        if (options[@"body"]){
            NSString *body = [RCTConvert NSString:options[@"body"]];
            [mail setMessageBody:body isHTML:isHTML];
        }

        if (options[@"recipients"]){
            NSArray *recipients = [RCTConvert NSArray:options[@"recipients"]];
            [mail setToRecipients:recipients];
        }

        if (options[@"ccRecipients"]){
            NSArray *ccRecipients = [RCTConvert NSArray:options[@"ccRecipients"]];
            [mail setCcRecipients:ccRecipients];
        }

        if (options[@"bccRecipients"]){
            NSArray *bccRecipients = [RCTConvert NSArray:options[@"bccRecipients"]];
            [mail setBccRecipients:bccRecipients];
        }

        if (options[@"attachmentList"]){
            NSArray *attachments = [RCTConvert NSArray:options[@"attachmentList"]];

      			for(NSDictionary *attachment in attachments){
      				NSString *path = [RCTConvert NSString:attachment[@"path"]];
      				NSString *type = [RCTConvert NSString:attachment[@"mimeType"]];
      				NSString *name = [RCTConvert NSString:attachment[@"name"]];

      				if (name == nil){
      					name = [[path lastPathComponent] stringByDeletingPathExtension];
      				}
      				// Get the resource path and read the file using NSData
      				NSData *fileData = [NSData dataWithContentsOfFile:path];

                    // Determine the MIME type
                    NSString *mimeType;

                    if ([type isEqualToString:@"jpg"]) {
                        mimeType = @"image/jpeg";
                    } else if ([type isEqualToString:@"png"]) {
                        mimeType = @"image/png";
                    } else if ([type isEqualToString:@"doc"]) {
                        mimeType = @"application/msword";
                    } else if ([type isEqualToString:@"docx"]) {
                        mimeType = @"application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    } else if ([type isEqualToString:@"ppt"]) {
                        mimeType = @"application/vnd.ms-powerpoint";
                    } else if ([type isEqualToString:@"pptx"]) {
                        mimeType = @"application/vnd.openxmlformats-officedocument.presentationml.presentation";
                    } else if ([type isEqualToString:@"html"]) {
                        mimeType = @"text/html";
                    } else if ([type isEqualToString:@"csv"]) {
                        mimeType = @"text/csv";
                    } else if ([type isEqualToString:@"pdf"]) {
                        mimeType = @"application/pdf";
                    } else if ([type isEqualToString:@"vcard"]) {
                        mimeType = @"text/vcard";
                    } else if ([type isEqualToString:@"json"]) {
                        mimeType = @"application/json";
                    } else if ([type isEqualToString:@"zip"]) {
                        mimeType = @"application/zip";
                    } else if ([type isEqualToString:@"text"]) {
                        mimeType = @"text/*";
                    } else if ([type isEqualToString:@"mp3"]) {
                        mimeType = @"audio/mpeg";
                    } else if ([type isEqualToString:@"wav"]) {
                        mimeType = @"audio/wav";
                    } else if ([type isEqualToString:@"aiff"]) {
                        mimeType = @"audio/aiff";
                    } else if ([type isEqualToString:@"flac"]) {
                        mimeType = @"audio/flac";
                    } else if ([type isEqualToString:@"ogg"]) {
                        mimeType = @"audio/ogg";
                    } else if ([type isEqualToString:@"xls"]) {
                        mimeType = @"application/vnd.ms-excel";
                    } else if ([type isEqualToString:@"xlsx"]) {
                        mimeType = @"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    }
      				[mail addAttachmentData:fileData mimeType:mimeType fileName:name];
      			}
        }

        UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];

        while (root.presentedViewController) {
            root = root.presentedViewController;
        }
        [root presentViewController:mail animated:YES completion:nil];
    } else {
        callback(@[@"not_available"]);
    }
}

#pragma mark MFMailComposeViewControllerDelegate Methods

- (void)mailComposeController:(MFMailComposeViewController *)controller didFinishWithResult:(MFMailComposeResult)result error:(NSError *)error
{
    NSString *key = RCTKeyForInstance(controller);
    RCTResponseSenderBlock callback = _callbacks[key];
    if (callback) {
        switch (result) {
            case MFMailComposeResultSent:
                callback(@[[NSNull null] , @"sent"]);
                break;
            case MFMailComposeResultSaved:
                callback(@[[NSNull null] , @"saved"]);
                break;
            case MFMailComposeResultCancelled:
                callback(@[[NSNull null] , @"cancelled"]);
                break;
            case MFMailComposeResultFailed:
                callback(@[@"failed"]);
                break;
            default:
                callback(@[@"error"]);
                break;
        }
        [_callbacks removeObjectForKey:key];
    } else {
        RCTLogWarn(@"No callback registered for mail: %@", controller.title);
    }
    UIViewController *ctrl = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
    while (ctrl.presentedViewController && ctrl != controller) {
        ctrl = ctrl.presentedViewController;
    }
    [ctrl dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark Private

static NSString *RCTKeyForInstance(id instance)
{
    return [NSString stringWithFormat:@"%p", instance];
}

@end
