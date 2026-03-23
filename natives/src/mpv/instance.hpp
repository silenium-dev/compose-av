#ifndef NATIVES_INSTANCE_HPP
#define NATIVES_INSTANCE_HPP

#include <memory>
#include <shared_mutex>
#include <mpv/client.h>
#include <string>

#define INSTANCE(ptr) const auto instance = reinterpret_cast<MPVInstance *>(ptr);

class MpvCallback;

class MPVInstance {
public:
    explicit MPVInstance();

    virtual ~MPVInstance();

    void setOption(const std::string &name, const std::string &value) const;

    void setOption(const std::string &name, int64_t value) const;

    void setOption(const std::string &name, bool value) const;

    void setOption(const std::string &name, double value) const;

    void setCallback(std::unique_ptr<MpvCallback> callback);

    void unsetCallback();

    void initialize() const;

private:
    mpv_handle *m_handle;
    std::shared_mutex m_callbackMutex;
    std::unique_ptr<MpvCallback> m_callback;
};

#endif //NATIVES_INSTANCE_HPP
