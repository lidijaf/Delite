#ifndef _DELITE_CONFIG_H_
#define _DELITE_CONFIG_H_

class Config {
public:
    int numThreads;
    int numCores;
    int numSockets;

    Config(int _numThreads) {
        numThreads = _numThreads;
        numCores = _numThreads;
        numSockets = 1;
    }

    Config(int _numThreads, int _numCores, int _numSockets) {
        numThreads = _numThreads;
        numCores = _numCores;
        numSockets = _numSockets;
    }

    // current strategy is to spill threads to a new socket only when full
    int threadsPerSocket() {
        return numCores / numSockets;
    }

    int threadToSocket(int threadId) {
        int socketId = (threadId / threadsPerSocket()) % numSockets;
        return socketId;
    }
};

#endif

