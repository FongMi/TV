#include <android/log.h>
#include <jni.h>
#include <pthread.h>
#include <unistd.h>

#include <cstdlib>
#include <cstring>
#include <string>

namespace node {
int Start(int argc, char *argv[]);
}

namespace {

constexpr const char *kLogTag = "NodeJS";
constexpr size_t kMaxLogLineBytes = 2048;
constexpr const char *kTruncatedSuffix = " ... [truncated]";

struct LogPipe {
    int descriptor[2]{};
    int priority{};
};

void *forward_log(void *value) {
    auto *pipe = static_cast<LogPipe *>(value);
    char buffer[2048];
    std::string line;
    bool truncated = false;
    ssize_t count;
    while ((count = read(pipe->descriptor[0], buffer, sizeof(buffer) - 1)) > 0) {
        for (ssize_t i = 0; i < count; ++i) {
            if (buffer[i] == '\n') {
                if (!line.empty() && line.back() == '\r') line.pop_back();
                if (truncated) line.append(kTruncatedSuffix);
                if (!line.empty()) __android_log_write(pipe->priority, kLogTag, line.c_str());
                line.clear();
                truncated = false;
            } else if (line.size() < kMaxLogLineBytes) {
                line.push_back(buffer[i]);
            } else {
                truncated = true;
            }
        }
    }
    if (truncated) line.append(kTruncatedSuffix);
    if (!line.empty()) __android_log_write(pipe->priority, kLogTag, line.c_str());
    close(pipe->descriptor[0]);
    delete pipe;
    return nullptr;
}

void redirect_descriptor(int target, int priority) {
    auto *log_pipe = new LogPipe{};
    log_pipe->priority = priority;
    if (pipe(log_pipe->descriptor) != 0) {
        delete log_pipe;
        return;
    }
    pthread_t thread;
    if (pthread_create(&thread, nullptr, forward_log, log_pipe) != 0) {
        close(log_pipe->descriptor[0]);
        close(log_pipe->descriptor[1]);
        delete log_pipe;
        return;
    }
    pthread_detach(thread);
    if (dup2(log_pipe->descriptor[1], target) < 0) {
        close(log_pipe->descriptor[1]);
        return;
    }
    close(log_pipe->descriptor[1]);
}

}  // namespace

extern "C" JNIEXPORT jint JNICALL
Java_com_fongmi_nodejs_NodeService_startNodeWithArguments(
        JNIEnv *env, jclass, jobjectArray arguments) {
    if (arguments == nullptr) return -1;
    const jsize argument_count = env->GetArrayLength(arguments);
    if (argument_count <= 0) return -1;

    size_t buffer_size = 0;
    for (jsize i = 0; i < argument_count; ++i) {
        auto argument = static_cast<jstring>(env->GetObjectArrayElement(arguments, i));
        if (argument == nullptr) return -1;
        buffer_size += static_cast<size_t>(env->GetStringUTFLength(argument)) + 1;
        env->DeleteLocalRef(argument);
    }

    auto *buffer = static_cast<char *>(calloc(buffer_size, sizeof(char)));
    auto **argv = static_cast<char **>(calloc(static_cast<size_t>(argument_count) + 1, sizeof(char *)));
    if (buffer == nullptr || argv == nullptr) {
        free(buffer);
        free(argv);
        return -1;
    }

    char *position = buffer;
    for (jsize i = 0; i < argument_count; ++i) {
        auto argument = static_cast<jstring>(env->GetObjectArrayElement(arguments, i));
        if (argument == nullptr) {
            free(argv);
            free(buffer);
            return -1;
        }
        const char *value = env->GetStringUTFChars(argument, nullptr);
        if (value == nullptr) {
            env->DeleteLocalRef(argument);
            free(argv);
            free(buffer);
            return -1;
        }
        const size_t length = strlen(value);
        memcpy(position, value, length);
        argv[i] = position;
        position += length + 1;
        env->ReleaseStringUTFChars(argument, value);
        env->DeleteLocalRef(argument);
    }

    redirect_descriptor(STDOUT_FILENO, ANDROID_LOG_INFO);
    redirect_descriptor(STDERR_FILENO, ANDROID_LOG_ERROR);
    const int result = node::Start(argument_count, argv);
    free(argv);
    free(buffer);
    return result;
}
