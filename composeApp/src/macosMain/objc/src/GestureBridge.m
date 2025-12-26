// GestureBridge.m
#import <Cocoa/Cocoa.h>

typedef void (*GestureCallback)(int phase, double deltaX, double deltaY);

static GestureCallback g_callback = NULL;

@interface GestureView : NSView
@property (nonatomic, strong) NSView *wrappedView;
@end

@implementation GestureView

- (void)scrollWheel:(NSEvent *)event {
    if (g_callback != NULL) {
        g_callback((int)event.phase, event.scrollingDeltaX, event.scrollingDeltaY);
    }
}

// AWT compatibility methods
- (BOOL)mouseIsOver {
    return NO;
}

- (BOOL)acceptsFirstMouse:(NSEvent *)event {
    return YES;
}

- (BOOL)acceptsFirstResponder {
    return YES;
}

// Ensure we receive all events
- (NSView *)hitTest:(NSPoint)point {
    NSView *result = [super hitTest:point];
    // Always return self for scroll events
    return result ? self : nil;
}

@end

void installGestureListener(NSWindow *window, GestureCallback cb) {
//    NSLog(@"installGestureListener called from thread: %@", [NSThread currentThread]);
//    NSLog(@"Is main thread: %d", [NSThread isMainThread]);
    
    // Ensure UI operations happen on the main thread
    dispatch_async(dispatch_get_main_queue(), ^{
//        NSLog(@"Inside dispatch_async, on main thread: %d", [NSThread isMainThread]);
        g_callback = cb;
        
        // Use local event monitor - most reliable approach
        [NSEvent addLocalMonitorForEventsMatchingMask:NSEventMaskScrollWheel handler:^NSEvent*(NSEvent *event) {
            NSLog(@"Scroll event, phase: %ld", (long)event.phase);
            
            if (g_callback != NULL) {
                g_callback((int)event.phase, event.scrollingDeltaX, event.scrollingDeltaY);
            }
            return event; // Pass through to the app
        }];
        
        NSLog(@"Gesture listener installed successfully");
    });
}
