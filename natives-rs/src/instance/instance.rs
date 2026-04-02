use crate::callback::{MpvCallback, MpvEvent, NoopCallback};
use jni::{Env, JavaVM};
use libmpv2_sys::*;
use std::sync::{Arc, Condvar, Mutex};
use std::thread::{JoinHandle, spawn};

pub struct MpvInstance {
    mpv_handle: *mut mpv_handle,
    jvm: JavaVM,

    event_dispatcher: Option<JoinHandle<()>>,
    inner: Arc<MpvInner>,
}

#[derive(Copy, Clone, Debug, Default, PartialEq, Eq, Hash)]
struct MpvEventLoopState {
    running: bool,
    wakeup: bool,
}

struct MpvInner {
    jvm: JavaVM,
    state: Mutex<MpvEventLoopState>,
    cv: Condvar,
    callback: Arc<dyn MpvCallback>,
}

impl MpvInner {
    fn set_callback(&mut self, callback: Arc<dyn MpvCallback>) {
        self.callback = callback
    }

    fn event_loop(&self, env: &mut Env) {
        loop {
            let mut guard = self
                .cv
                .wait_while(self.state.lock().expect("mutex was poisoned"), |state| {
                    !state.wakeup && state.running
                })
                .expect("cv wait failed");
            if !guard.running {
                println!("Event loop stopped");
                break;
            }
            guard.wakeup = false;
            let value = env
                .new_string("test")
                .expect("failed to create string")
                .into();
            self.callback.handle(
                env,
                MpvEvent::PropertyChanged {
                    name: "test".into(),
                    value,
                },
            );
        }
    }
}

impl MpvInstance {
    pub fn new(jvm: JavaVM) -> Self {
        let handle = unsafe { mpv_create() };
        if handle.is_null() {
            panic!("Failed to create mpv instance");
        }
        let inner = Arc::from(MpvInner {
            jvm: jvm.clone(),
            cv: Condvar::new(),
            state: Mutex::new(MpvEventLoopState {
                running: true,
                wakeup: false,
            }),
            callback: Arc::new(NoopCallback::new()),
        });
        let spawn_inner = inner.clone();
        let spawn_jvm = jvm.clone();
        let dispatcher = spawn(move || {
            spawn_jvm
                .attach_current_thread(|env| {
                    Ok::<(), jni::errors::Error>(spawn_inner.event_loop(env))
                })
                .expect("Failed to attach thread to JVM")
        });
        Self {
            mpv_handle: handle,
            jvm,
            event_dispatcher: Some(dispatcher),
            inner,
        }
    }
}

impl Drop for MpvInstance {
    fn drop(&mut self) {
        println!("Dropping MpvInstance");
        {
            let mut guard = self.inner.state.lock().expect("mutex was poisoned");
            guard.running = false;
        }

        self.inner.cv.notify_all();
        self.event_dispatcher
            .take()
            .unwrap()
            .join()
            .expect("Failed to join event dispatcher thread");
        unsafe { mpv_destroy(self.mpv_handle) };
    }
}
