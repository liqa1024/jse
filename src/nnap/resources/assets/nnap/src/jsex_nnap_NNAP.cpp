#include "jsex_nnap_NNAP.h"
#include "jniutil.h"

#include <torch/torch.h>
#include <torch/script.h>


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

}
