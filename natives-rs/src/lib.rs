use crate::instance::MpvInstance;
use jni::objects::JObject;
use jni::strings::JNIString;
use jni::sys::jlong;
use jni::{Env, EnvUnowned, Outcome, jni_mangle, jni_sig};
use libc::LC_NUMERIC;
use std::ffi::CString;

mod callback;
mod instance;

#[jni_mangle("dev.silenium.multimedia.core.mpv.MPVKt", "createN")]
pub fn createN<'local>(mut env: EnvUnowned<'local>, _this: JObject<'local>) -> JObject<'local> {
    let mpv: Outcome<_, jni::errors::Error> = env
        .with_env(|env| {
            let jvm = env.get_java_vm().expect("JVM not found");
            let cLocale = CString::new("C").unwrap();
            unsafe { libc::setlocale(LC_NUMERIC, cLocale.as_ptr()) };
            Ok(Box::new(MpvInstance::new(jvm)))
        })
        .into_outcome();
    let result = match mpv {
        Outcome::Ok(mpv) => env
            .with_env(|env| Ok::<JObject, jni::errors::Error>(boxed_ptr(env, Box::into_raw(mpv))))
            .resolve::<jni::errors::ThrowRuntimeExAndDefault>(),
        Outcome::Err(e) => {
            env.with_env(|env| {
                let runtimeException = env
                    .find_class(JNIString::from("java/lang/RuntimeException"))
                    .expect("RuntimeException not found");
                env.throw_new(runtimeException, JNIString::from(e.to_string()))
                    .expect("Failed to throw exception");
                println!("MPV creation failed: {}", e);
                Ok::<JObject, jni::errors::Error>(result_failure(env, e.to_string().as_str()))
            })
            .resolve::<jni::errors::ThrowRuntimeExAndDefault>();
            JObject::null()
        }
        Outcome::Panic(e) => panic!("MPV creation panicked: {:?}", e),
    };
    println!("MPV created: {:x}", result.addr());
    result
}

#[jni_mangle("dev.silenium.multimedia.core.mpv.MPVKt", "destroyN")]
pub fn destroyN<'local>(
    _env: EnvUnowned<'local>,
    _this: JObject<'local>,
    handle: *mut MpvInstance,
) {
    println!("Destroying MPV: {:?}", handle);
    let mpv = unsafe { Box::from_raw(handle) };
    if handle.is_null() {
        return;
    }
    drop(mpv);
}

fn result_failure<'local>(env: &mut Env<'local>, message: &str) -> JObject<'local> {
    let runtime_exception = env
        .find_class(JNIString::from("java/lang/RuntimeException"))
        .expect("RuntimeException not found");
    let message_obj = env
        .new_string(message)
        .expect("Failed to create String object");
    let exception = env
        .new_object(
            runtime_exception,
            jni_sig!((java.lang.String) -> void),
            &[jni::JValue::Object(&(message_obj.into()))],
        )
        .expect("Failed to create RuntimeException object");
    let result_class = env
        .find_class(JNIString::from("kotlin/lang/Result$Failure"))
        .expect("Result.Failure not found");
    env.new_object(
        result_class,
        jni_sig!((java.lang.Throwable) -> void),
        &[jni::JValue::Object(&exception)],
    )
    .expect("Failed to create Result.Failure object")
}

fn boxed_ptr<'local, P>(env: &mut Env<'local>, ptr: *mut P) -> JObject<'local> {
    let class = env
        .find_class(JNIString::from("java/lang/Long"))
        .expect("Long class not found");
    env.new_object(
        class,
        jni_sig!((jlong) -> void),
        &[jni::JValue::Long(ptr as jlong)],
    )
    .expect("Failed to find Long constructor")
}
