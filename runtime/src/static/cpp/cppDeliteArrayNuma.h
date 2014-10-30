#ifndef _CPP_DELITEARRAYNUMA_H_
#define _CPP_DELITEARRAYNUMA_H_

#include <stdlib.h>
#include <string.h>
#include <math.h>

#include "DeliteCpp.h"

template <class T>
class cppDeliteArrayNuma {
public:
    T **wrapper;
    T *avg;
    int length;
    int numGhostCells; // constant for all internal arrays

    // Constructor
    cppDeliteArrayNuma(int _length, int _numGhostCells) {
        length = _length;
        numGhostCells = _numGhostCells;
	avg = (T *)malloc(numGhostCells*sizeof(T));
        wrapper = (T **)malloc(config->numSockets*sizeof(T*));
    }

    T apply(int idx) {
      return applyAt(0, idx);
    }

    void update(int idx, T value) {
      updateAt(0, idx, value);
    }

    int internalLength() {
      return std::min(length, length/config->numSockets+numGhostCells);
    }

    // Additional functions
    // void copy(int srcOffset, cppDeliteArray<T> *dest, int destOffset, int length) {
    //   memcpy(dest->data + destOffset, data + srcOffset, sizeof(T) * length);
    // }

    // cppDeliteArray<T> *take(int n) {
    //   cppDeliteArray<T> *result = new cppDeliteArray<T>(n);
    //   memcpy(result->data, data, sizeof(T) * n);
    //   return result;
    // }

    // NUMA-specific functionality
    void allocInternal(int tid) {
      // TODO: this ghostCell stuff is half-baked right now
      //printf("allocating array for socket %d\n", config->threadToSocket(tid));
      wrapper[config->threadToSocket(tid)] = (T*)malloc(internalLength()*sizeof(T));
      memset(wrapper[config->threadToSocket(tid)], 0, internalLength()*sizeof(T));
    }

    T applyAt(int tid, int i) {
      return wrapper[config->threadToSocket(tid)][i];
    }

    void updateAt(int tid, int i, T value) {
      wrapper[config->threadToSocket(tid)][i] = value;
    }

    void combineAverage() {
      // calls to this should only be generated on type T <: Numeric, as checked inside the Delite compiler
      memset(avg, 0, numGhostCells*sizeof(T));
      int start = internalLength() - numGhostCells;
      int numActiveSockets = config->activeSockets();
      //printf("start: %d, numGhostCells: %d, internalLnehgt: %d, numActiveSockets: %d\n", start, numGhostCells, internalLength(), numActiveSockets);
      for (int s = 0; s < numActiveSockets; s++) {
        // currently only ghosting "to the right"
	for (int i = start; i < numGhostCells+start; i++) {
          avg[i-start] += wrapper[s][i];
        }
      }

      for (int i = 0; i < numGhostCells; i++) {
        avg[i] = avg[i] / numActiveSockets;
      }

      for (int s = 0; s < numActiveSockets; s++) {
	for (int i = start; i < numGhostCells+start; i++) {
	  wrapper[s][i] = avg[i-start];
	}        
      }
    }

    // --
    void release(void);
};

#endif
