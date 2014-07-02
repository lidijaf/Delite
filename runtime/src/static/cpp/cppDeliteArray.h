#ifndef _CPP_DELITEARRAY_H_
#define _CPP_DELITEARRAY_H_

#include <stdlib.h>
#include <string.h>

/*
template <class T>
class cppDeliteArray {
public:
    T *data;
    size_t length;

    // Constructor
    cppDeliteArray(size_t _length) {
        length = _length;
        //TODO: remove initialization to zero
        data = new T[length]();
    }

    cppDeliteArray(T *_data, size_t _length) {
        length = _length;
        data = _data;
    }

    T apply(size_t idx) {
        return data[idx];
    }

    void update(size_t idx, T value) {
        data[idx] = value;
    }

    // DeliteCollection
    size_t size() {
        return length;
    }

    T dcApply(size_t idx) {
        return data[idx];
    }

    void dcUpdate(size_t idx, T value) {
        data[idx] = value;
    }
    
    // Additional functions
    void copy(int srcOffset, cppDeliteArray<T> *dest, int destOffset, int length) {
      if ((data==dest->data) && (srcOffset<destOffset))
        std::copy_backward(data+srcOffset, data+srcOffset+length, dest->data+destOffset+length);
      else
        std::copy(data+srcOffset, data+srcOffset+length, dest->data+destOffset);
    }

    cppDeliteArray<T> *arrayunion(cppDeliteArray<T> *rhs) {
      size_t newLength = length + rhs->length;
      cppDeliteArray<T> *result = new cppDeliteArray<T>(newLength);
      size_t acc = 0;
      for(size_t i=0; i<length; i++) {
        T elem = data[i];
        size_t j = 0;
        while(j < acc) {
          if(elem == result->data[j]) break;
          j += 1;
        }
        if(j == acc) result->data[acc++] = elem;
      }
      for(size_t i=0; i<rhs->length; i++) {
        T elem = rhs->data[i];
        size_t j = 0;
        while(j < acc) {
          if(elem == result->data[j]) break;
          j += 1;
        }
        if(j == acc) result->data[acc++] = elem;
      }
      result->length = acc-1;
      //TODO: Need to shrink the actual array size?
      return result;
    }

    cppDeliteArray<T> *intersect(cppDeliteArray<T> *rhs) {
      size_t newLength = max(length, rhs->length);
      cppDeliteArray<T> *result = new cppDeliteArray<T>(newLength);
      size_t acc = 0;
      for(size_t i=0; i<length; i++)
        for(size_t j=0; j<rhs->length; j++) 
          if(data[i] == rhs->data[j]) result->data[acc++] = data[i];
      result->length = acc-1;
      //TODO: Need to shrink the actual array size?
      return result;
    }
    
    cppDeliteArray<T> *take(size_t n) {
      cppDeliteArray<T> *result = new cppDeliteArray<T>(n);
      memcpy(result->data, data, sizeof(T) * n);
      return result;
    }

    void release(void);
};
*/
#endif
