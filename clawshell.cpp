#include <errno.h>
#include <fcntl.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/un.h>
#include <unistd.h>

#include <iostream>
#include <string>

static std::string ReadAllStdin() {
    std::string input;
    std::string line;
    while (std::getline(std::cin, line)) {
        input.append(line);
        input.push_back('\n');
    }
    if (!input.empty() && input.back() == '\n') {
        input.pop_back();
    }
    return input;
}

static bool SetTimeout(int fd, int millis) {
    struct timeval tv;
    tv.tv_sec = millis / 1000;
    tv.tv_usec = (millis % 1000) * 1000;
    if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv)) < 0) {
        return false;
    }
    if (setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv)) < 0) {
        return false;
    }
    return true;
}

static int ConnectSocket(const char* path) {
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) {
        return -1;
    }
    sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, path, sizeof(addr.sun_path) - 1);
    if (connect(fd, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) {
        close(fd);
        return -1;
    }
    return fd;
}

static int ConnectAbstractSocket(const char* name) {
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) {
        return -1;
    }
    sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    size_t name_len = strlen(name);
    if (name_len + 1 >= sizeof(addr.sun_path)) {
        close(fd);
        return -1;
    }
    addr.sun_path[0] = '\0';
    memcpy(addr.sun_path + 1, name, name_len);
    socklen_t addr_len = static_cast<socklen_t>(offsetof(sockaddr_un, sun_path) + 1 + name_len);
    if (connect(fd, reinterpret_cast<sockaddr*>(&addr), addr_len) < 0) {
        close(fd);
        return -1;
    }
    return fd;
}

static bool WriteAll(int fd, const std::string& data) {
    const char* buf = data.c_str();
    size_t left = data.size();
    while (left > 0) {
        ssize_t n = write(fd, buf, left);
        if (n < 0) {
            if (errno == EINTR) {
                continue;
            }
            return false;
        }
        buf += n;
        left -= static_cast<size_t>(n);
    }
    return true;
}

static std::string ReadAll(int fd) {
    std::string out;
    char buf[4096];
    while (true) {
        ssize_t n = read(fd, buf, sizeof(buf));
        if (n < 0) {
            if (errno == EINTR) {
                continue;
            }
            break;
        }
        if (n == 0) {
            break;
        }
        out.append(buf, buf + n);
    }
    return out;
}

int main() {
    const char* kSocketPath = "/dev/socket/claw_worker";
    const char* kSocketName = "claw_worker";
    const char* kTmpSocketPath = "/data/local/tmp/claw_worker.sock";
    std::string payload = ReadAllStdin();
    int fd = ConnectAbstractSocket(kSocketName);
    if (fd < 0) {
        fd = ConnectSocket(kSocketPath);
    }
    if (fd < 0) {
        fd = ConnectSocket(kTmpSocketPath);
    }
    if (fd < 0) {
        std::cerr << "connect_failed abstract:" << kSocketName
                  << " fs:" << kSocketPath
                  << " fs:" << kTmpSocketPath << "\n";
        return 1;
    }
    SetTimeout(fd, 5000);
    if (!WriteAll(fd, payload)) {
        std::cerr << "write_failed\n";
        close(fd);
        return 1;
    }
    shutdown(fd, SHUT_WR);
    std::string response = ReadAll(fd);
    close(fd);
    if (!response.empty()) {
        std::cout << response;
    }
    return 0;
}
