#include "jsex_nnap_NNAP.h"
#include "jniutil.h"

#include <torch/torch.h>
#include <torch/script.h>
#include <strstream>


extern "C" {

JNIEXPORT void JNICALL Java_jsex_nnap_NNAP_setSingleThread0(JNIEnv *aEnv, jclass aClazz) {
    try {
        torch::set_num_threads(1);
        torch::set_num_interop_threads(1);
    } catch (const std::exception &e) {
        throwExceptionTorch(aEnv, e.what());
    }
}

JNIEXPORT jlong JNICALL Java_jsex_nnap_NNAP_load0(JNIEnv *aEnv, jclass aClazz, jstring aModelPath) {
    char *tModelPath = parseStr(aEnv, aModelPath);
    torch::jit::Module *tModulePtr = NULL;
    try {
        tModulePtr = new torch::jit::Module(torch::jit::load(tModelPath, at::kCPU));
        tModulePtr->eval();
    } catch (const std::exception &e) {
        throwExceptionTorch(aEnv, e.what());
    }
    FREE(tModelPath);
    return (jlong)(intptr_t)tModulePtr;
}

JNIEXPORT jlong JNICALL Java_jsex_nnap_NNAP_load1(JNIEnv *aEnv, jclass aClazz, jbyteArray aModelBytes, jint aSize) {
    void *tBuf = aEnv->GetPrimitiveArrayCritical(aModelBytes, NULL);
    torch::jit::Module *tModulePtr = NULL;
    try {
        // This is the only way I have found to use buf input, but this happens to be deprecated, which is c++
        // Note: std::istringstream is no use
        std::strstreambuf tBufStream((char *)tBuf, aSize*sizeof(jbyte));
        std::istream tInStream(&tBufStream);
        tModulePtr = new torch::jit::Module(torch::jit::load(tInStream, at::kCPU));
        tModulePtr->eval();
    } catch (const std::exception &e) {
        throwExceptionTorch(aEnv, e.what());
    }
    aEnv->ReleasePrimitiveArrayCritical(aModelBytes, tBuf, JNI_ABORT);
    return (jlong)(intptr_t)tModulePtr;
}

JNIEXPORT void JNICALL Java_jsex_nnap_NNAP_shutdown0(JNIEnv *aEnv, jclass aClazz, jlong aModelPtr) {
    delete (torch::jit::Module *)(intptr_t)aModelPtr;
}


JNIEXPORT jdouble JNICALL Java_jsex_nnap_NNAP_forward0(JNIEnv *aEnv, jclass aClazz, jlong aModelPtr, jdoubleArray aX, jint aStart, jint aCount) {
    torch::jit::Module *tModulePtr = (torch::jit::Module *)(intptr_t)aModelPtr;
    jboolean tAnyErr = JNI_FALSE;
    torch::Tensor tXTensor;
    try {
        tXTensor = torch::empty({1, aCount}, torch::TensorOptions(at::kCPU).dtype(at::kDouble));
        parsejdouble2doubleV(aEnv, aX, aStart, tXTensor.contiguous().mutable_data_ptr<double>(), 0, aCount);
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return 0.0;
    
    double tY;
    try {
        torch::NoGradGuard no_grad;
        tY = tModulePtr->forward({tXTensor}).toTensor().item<double>();
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return 0.0;
    return tY;
}
JNIEXPORT jdouble JNICALL Java_jsex_nnap_NNAP_forward1(JNIEnv *aEnv, jclass aClazz, jlong aModelPtr, jlong aX, jint aCount) {
    torch::jit::Module *tModulePtr = (torch::jit::Module *)(intptr_t)aModelPtr;
    jboolean tAnyErr = JNI_FALSE;
    torch::Tensor tXTensor;
    try {
        tXTensor = torch::from_blob((double *)(intptr_t)aX, {1, aCount}, torch::TensorOptions(at::kCPU).dtype(at::kDouble));
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return 0.0;
    
    double tY;
    try {
        torch::NoGradGuard no_grad;
        tY = tModulePtr->forward({tXTensor}).toTensor().item<double>();
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return 0.0;
    return tY;
}
JNIEXPORT void JNICALL Java_jsex_nnap_NNAP_batchForward0(JNIEnv *aEnv, jclass aClazz, jlong aModelPtr, jdoubleArray aX, jint aStart, jint aCount, jdoubleArray rY, jint rYStart, jint aBatchSize) {
    torch::jit::Module *tModulePtr = (torch::jit::Module *)(intptr_t)aModelPtr;
    jboolean tAnyErr = JNI_FALSE;
    torch::Tensor tXTensor;
    try {
        tXTensor = torch::empty({aBatchSize, aCount}, torch::TensorOptions(at::kCPU).dtype(at::kDouble));
        parsejdouble2doubleV(aEnv, aX, aStart, tXTensor.contiguous().mutable_data_ptr<double>(), 0, aBatchSize*aCount);
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return;
    
    try {
        torch::NoGradGuard no_grad;
        torch::Tensor tYTensor = tModulePtr->forward({tXTensor}).toTensor();
        parsedouble2jdoubleV(aEnv, rY, rYStart, tYTensor.contiguous().mutable_data_ptr<double>(), 0, aBatchSize);
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return;
}
JNIEXPORT void JNICALL Java_jsex_nnap_NNAP_batchForward1(JNIEnv *aEnv, jclass aClazz, jlong aModelPtr, jlong aX, jint aCount, jlong rY, jint aBatchSize) {
    torch::jit::Module *tModulePtr = (torch::jit::Module *)(intptr_t)aModelPtr;
    jboolean tAnyErr = JNI_FALSE;
    torch::Tensor tXTensor;
    try {
        tXTensor = torch::from_blob((double *)(intptr_t)aX, {aBatchSize, aCount}, torch::TensorOptions(at::kCPU).dtype(at::kDouble));
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return;
    
    try {
        torch::NoGradGuard no_grad;
        torch::Tensor tYTensor = tModulePtr->forward({tXTensor}).toTensor();
        std::memcpy((double *)(intptr_t)rY, tYTensor.contiguous().mutable_data_ptr<double>(), aBatchSize*sizeof(double));
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return;
}

JNIEXPORT jdouble JNICALL Java_jsex_nnap_NNAP_backward0(JNIEnv *aEnv, jclass aClazz, jlong aModelPtr, jdoubleArray aX, jint aStart, jdoubleArray rGradX, jint rStart, jint aCount) {
    torch::jit::Module *tModulePtr = (torch::jit::Module *)(intptr_t)aModelPtr;
    jboolean tAnyErr = JNI_FALSE;
    torch::Tensor tXTensor;
    try {
        tXTensor = torch::empty({1, aCount}, torch::TensorOptions(at::kCPU).dtype(at::kDouble).requires_grad(true));
        parsejdouble2doubleV(aEnv, aX, aStart, tXTensor.contiguous().mutable_data_ptr<double>(), 0, aCount);
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return 0.0;
    
    double tY;
    try {
        torch::Tensor tYTensor = tModulePtr->forward({tXTensor}).toTensor();
        tY = tYTensor.item<double>();
        tYTensor.backward();
        parsedouble2jdoubleV(aEnv, rGradX, rStart, tXTensor.grad().contiguous().const_data_ptr<double>(), 0, aCount);
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return 0.0;
    return tY;
}
JNIEXPORT jdouble JNICALL Java_jsex_nnap_NNAP_backward1(JNIEnv *aEnv, jclass aClazz, jlong aModelPtr, jlong aX, jlong rGradX, jint aCount) {
    torch::jit::Module *tModulePtr = (torch::jit::Module *)(intptr_t)aModelPtr;
    jboolean tAnyErr = JNI_FALSE;
    torch::Tensor tXTensor;
    try {
        tXTensor = torch::from_blob((double *)(intptr_t)aX, {1, aCount}, torch::TensorOptions(at::kCPU).dtype(at::kDouble).requires_grad(true));
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return 0.0;
    
    double tY;
    try {
        torch::Tensor tYTensor = tModulePtr->forward({tXTensor}).toTensor();
        tY = tYTensor.item<double>();
        tYTensor.backward();
        std::memcpy((double *)(intptr_t)rGradX, tXTensor.grad().contiguous().const_data_ptr<double>(), aCount*sizeof(double));
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return 0.0;
    return tY;
}
JNIEXPORT void JNICALL Java_jsex_nnap_NNAP_batchBackward0(JNIEnv *aEnv, jclass aClazz, jlong aModelPtr, jdoubleArray aX, jint aStart, jdoubleArray rGradX, jint rStart, jint aCount, jdoubleArray rY, jint rYStart, jint aBatchSize) {
    torch::jit::Module *tModulePtr = (torch::jit::Module *)(intptr_t)aModelPtr;
    jboolean tAnyErr = JNI_FALSE;
    torch::Tensor tXTensor;
    try {
        tXTensor = torch::empty({aBatchSize, aCount}, torch::TensorOptions(at::kCPU).dtype(at::kDouble).requires_grad(true));
        parsejdouble2doubleV(aEnv, aX, aStart, tXTensor.contiguous().mutable_data_ptr<double>(), 0, aBatchSize*aCount);
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return;
    
    try {
        torch::Tensor tYTensor = tModulePtr->forward({tXTensor}).toTensor();
        if (rY != NULL) parsedouble2jdoubleV(aEnv, rY, rYStart, tYTensor.contiguous().mutable_data_ptr<double>(), 0, aBatchSize);
        tYTensor.sum().backward();
        parsedouble2jdoubleV(aEnv, rGradX, rStart, tXTensor.grad().contiguous().const_data_ptr<double>(), 0, aBatchSize*aCount);
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return;
}
JNIEXPORT void JNICALL Java_jsex_nnap_NNAP_batchBackward1(JNIEnv *aEnv, jclass aClazz, jlong aModelPtr, jlong aX, jlong rGradX, jint aCount, jlong rY, jint aBatchSize) {
    torch::jit::Module *tModulePtr = (torch::jit::Module *)(intptr_t)aModelPtr;
    jboolean tAnyErr = JNI_FALSE;
    torch::Tensor tXTensor;
    try {
        tXTensor = torch::from_blob((double *)(intptr_t)aX, {aBatchSize, aCount}, torch::TensorOptions(at::kCPU).dtype(at::kDouble).requires_grad(true));
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return;
    
    try {
        torch::Tensor tYTensor = tModulePtr->forward({tXTensor}).toTensor();
        double *rYBuf = (double *)(intptr_t)rY;
        if (rYBuf != NULL) std::memcpy(rYBuf, tYTensor.contiguous().mutable_data_ptr<double>(), aBatchSize*sizeof(double));
        tYTensor.sum().backward();
        std::memcpy((double *)(intptr_t)rGradX, tXTensor.grad().contiguous().const_data_ptr<double>(), aBatchSize*aCount*sizeof(double));
    } catch (const std::exception &e) {
        tAnyErr = JNI_TRUE;
        throwExceptionTorch(aEnv, e.what());
    }
    if (tAnyErr) return;
}

}
