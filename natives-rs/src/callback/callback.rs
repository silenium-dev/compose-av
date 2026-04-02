use jni::Env;
use jni::objects::JObject;

pub enum MpvEvent<'local> {
    PropertyChanged {
        name: String,
        value: JObject<'local>,
    },
    PropertyGet {
        subscription_id: i64,
        value: JObject<'local>,
    },
    PropertySet {
        subscription_id: i64,
        value: JObject<'local>,
    },
    CommandReply {
        subscription_id: i64,
        value: JObject<'local>,
    },
}

pub trait MpvCallback: Send + Sync {
    fn handle(&self, env: &mut Env, event: MpvEvent);
}

pub struct NoopCallback;

impl NoopCallback {
    pub fn new() -> Self {
        Self
    }
}

impl MpvCallback for NoopCallback {
    fn handle(&self, _: &mut Env, _: MpvEvent) {}
}
