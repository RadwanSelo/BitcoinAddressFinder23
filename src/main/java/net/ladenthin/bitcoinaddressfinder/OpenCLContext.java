// @formatter:off
/**
 * Copyright 2020 Bernard Ladenthin bernard.ladenthin@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
// @formatter:on
package net.ladenthin.bitcoinaddressfinder;

import com.google.common.io.Resources;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.ladenthin.bitcoinaddressfinder.configuration.CProducerOpenCL;
import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateCommandQueueWithProperties;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import org.jocl.cl_queue_properties;
import org.jocl.CL;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenCLContext {

    // kernel modes that do not write all intermediate results into the result buffer:
    public static final int GEN_XY_COORDINATES_ONLY_MODE = 0;
    public static final int GEN_PUBLIC_KEY_ONLY_MODE = 1;
    public static final int GEN_RIPEMD160_ONLY_MODE = 2;
    public static final int GEN_ADDRESSES_ONLY_MODE = 3;

    // kernel modes that write all intermediate results into the result buffer:
    public static final int GEN_UNTIL_1ST_SHA256_MODE = 4;
    public static final int GEN_UNTIL_RIPEMD160_MODE = 5;
    public static final int GEN_UNTIL_2ND_SHA256_MODE = 6;
    public static final int GEN_UNTIL_3RD_SHA256_MODE = 7;
    public static final int GEN_UNTIL_ADDRESS_MODE = 8;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    private final int[] errorCode = new int[1];

    public String[] getOpenCLPrograms() throws IOException {
        List<String> resourceNamesContent = getResourceNamesContent(getResourceNames());
        List<String> resourceNamesContentWithReplacements = new ArrayList<>();
        for (String content : resourceNamesContent) {
            String contentWithReplacements = content;
            contentWithReplacements = contentWithReplacements.replaceAll("#include.*", "");
            contentWithReplacements = contentWithReplacements.replaceAll("GLOBAL_AS const secp256k1_t \\*tmps", "const secp256k1_t \\*tmps");
            resourceNamesContentWithReplacements.add(contentWithReplacements);
        }
        String[] openClPrograms = resourceNamesContentWithReplacements.toArray(new String[0]);
        return openClPrograms;
    }

    private List<String> getResourceNames() {
        List<String> resourceNames = new ArrayList<>();
        resourceNames.add("inc_defines.h");
        resourceNames.add("copyfromhashcat/inc_vendor.h");
        resourceNames.add("copyfromhashcat/inc_types.h");
        resourceNames.add("copyfromhashcat/inc_platform.h");
        resourceNames.add("copyfromhashcat/inc_platform.cl");
        resourceNames.add("copyfromhashcat/inc_common.h");
        resourceNames.add("copyfromhashcat/inc_common.cl");

        resourceNames.add("copyfromhashcat/inc_ecc_secp256k1.h");
        resourceNames.add("copyfromhashcat/inc_ecc_secp256k1.cl");
        resourceNames.add("copyfromhashcat/inc_hash_sha256.h");
        resourceNames.add("copyfromhashcat/inc_hash_sha256.cl");
        resourceNames.add("copyfromhashcat/inc_hash_ripemd160.h");
        resourceNames.add("copyfromhashcat/inc_hash_ripemd160.cl");
        resourceNames.add("generator_utilities.cl");
        resourceNames.add("inc_ecc_secp256k1custom.cl");
        resourceNames.add("generate_btc_address.cl");
        resourceNames.add("generate_btc_ripemd160.cl");

        return resourceNames;
    }

    // names of kernel methods that only write the x,y-coordinates into the result buffer:
    private static final String ONLY_XY_COORDINATES_CHUNK_KERNEL = "generateKeyChunkKernel_grid";
    private static final String ONLY_XY_COORDINATES_NONCHUNK_KERNEL = "generateKeysKernel_grid";

    // names of kernel methods that only write the private key and the public key into the result buffer:
    private static final String UNTIL_PUBLIC_KEY_CHUNK_KERNEL = "generate_chunk_until_publickey";
    private static final String UNTIL_PUBLIC_KEY_NONCHUNK_KERNEL = "generate_until_publickey";

    // names of kernel methods that only write the private key and the RIPEMD-160 hash into the result buffer:
    private static final String ONLY_RIPEMD160_CHUNK_KERNEL = "generate_ripemd160_chunk";
    private static final String ONLY_RIPEMD160_NONCHUNK_KERNEL = "generate_ripemd160_nonchunk";

    // names of kernel methods that only write the private key and the address into the result buffer:
    private static final String ONLY_ADDRESS_CHUNK_KERNEL = "generate_address_chunk";
    private static final String ONLY_ADDRESS_NONCHUNK_KERNEL = "generate_address";

    // names of kernel methods that write all intermediate results into the result buffer:
    private static final String UNTIL_1ST_SHA256_CHUNK_KERNEL = "generate_chunk_until_first_sha256";
    private static final String UNTIL_1ST_SHA256_NONCHUNK_KERNEL = "generate_until_first_sha256";
    private static final String UNTIL_RIPEMD160_CHUNK_KERNEL = "generate_chunk_until_ripemd160";
    private static final String UNTIL_RIPEMD160_NONCHUNK_KERNEL = "generate_until_ripemd160";
    private static final String UNTIL_2ND_SHA256_CHUNK_KERNEL = "generate_chunk_until_second_sha256";
    private static final String UNTIL_2ND_SHA256_NONCHUNK_KERNEL = "generate_until_second_sha256";
    private static final String UNTIL_3RD_SHA256_CHUNK_KERNEL = "generate_chunk_until_third_sha256";
    private static final String UNTIL_3RD_SHA256_NONCHUNK_KERNEL = "generate_until_third_sha256";
    private static final String UNTIL_ADDRESS_CHUNK_KERNEL = "generate_chunk_until_address";
    private static final String UNTIL_ADDRESS_NONCHUNK_KERNEL = "generate_until_address";

    private final static boolean EXCEPTIONS_ENABLED = true;
    
    private final CProducerOpenCL producerOpenCL;

    private cl_context_properties contextProperties;
    private cl_device_id device;
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;
    private cl_kernel kernel;
    private OpenClTask openClTask;
    
    public OpenCLContext(CProducerOpenCL producerOpenCL) {
        this.producerOpenCL = producerOpenCL;
    }
    
    /**
     * Sets all properties and parameters to finally create the OpenCL kernel.
     *
     * @throws IOException When an error occurs while reading a resource.
     */
    public void init() throws IOException, UnknownKernelModeException {
        
        // #################### general ####################
        
        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(EXCEPTIONS_ENABLED);
        
        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];
        
        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[producerOpenCL.platformIndex];
        
        // Initialize the context properties
        contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
        
        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, producerOpenCL.deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        
        // Obtain a device ID 
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, producerOpenCL.deviceType, numDevices, devices, null);
        device = devices[producerOpenCL.deviceIndex];
        cl_device_id[] cl_device_ids = new cl_device_id[]{device};
        
        // Create a context for the selected device
        context = clCreateContext(contextProperties, 1, cl_device_ids, null, null, null);
        
        // Create a command-queue for the selected device
        cl_queue_properties properties = new cl_queue_properties();
        commandQueue = clCreateCommandQueueWithProperties(context, device, properties, null);
        
        // #################### kernel specifix ####################
        
        String[] openCLPrograms = getOpenCLPrograms();
        // Create the program from the source code
        program = clCreateProgramWithSource(context, openCLPrograms.length, openCLPrograms, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);
        
        // Create the kernel
        setKernel();

        openClTask = new OpenClTask(context, producerOpenCL);
    }

    private void setKernel() throws UnknownKernelModeException {
        if (producerOpenCL.kernelMode == GEN_XY_COORDINATES_ONLY_MODE) {
            setPublicKeyGeneratorKernel();
        } else if (producerOpenCL.kernelMode == GEN_RIPEMD160_ONLY_MODE) {
            setRipemd160GeneratorKernel();
        } else if (producerOpenCL.kernelMode == GEN_ADDRESSES_ONLY_MODE) {
            setAddressGeneratorKernel();
        } else if (producerOpenCL.kernelMode == GEN_PUBLIC_KEY_ONLY_MODE) {
            setBytewisePublicKeyKernel();
        } else if (producerOpenCL.kernelMode == GEN_UNTIL_1ST_SHA256_MODE) {
            setBytewiseFirstSha256Kernel();
        } else if (producerOpenCL.kernelMode == GEN_UNTIL_RIPEMD160_MODE) {
            setBytewiseRipemd160Kernel();
        } else if (producerOpenCL.kernelMode == GEN_UNTIL_2ND_SHA256_MODE) {
            setBytewiseSecondSha256Kernel();
        } else if (producerOpenCL.kernelMode == GEN_UNTIL_3RD_SHA256_MODE) {
            setBytewiseThirdSha256Kernel();
        } else if (producerOpenCL.kernelMode == GEN_UNTIL_ADDRESS_MODE) {
            setBytewiseAddressKernel();
        } else {
            throw new UnknownKernelModeException(producerOpenCL.kernelMode);
        }
    }

    private void setPublicKeyGeneratorKernel() {
        if (producerOpenCL.chunkMode) {
            kernel = clCreateKernel(program, ONLY_XY_COORDINATES_CHUNK_KERNEL, errorCode);
        } else {
            kernel = clCreateKernel(program, ONLY_XY_COORDINATES_NONCHUNK_KERNEL, errorCode);
        }
    }

    private void setRipemd160GeneratorKernel() throws UnknownKernelModeException {
        if (producerOpenCL.chunkMode) {
            kernel = clCreateKernel(program, ONLY_RIPEMD160_CHUNK_KERNEL, errorCode);
        } else {
            kernel = clCreateKernel(program, ONLY_RIPEMD160_NONCHUNK_KERNEL, errorCode);
        }
    }

    private void setAddressGeneratorKernel() {
        if (producerOpenCL.chunkMode) {
            kernel = clCreateKernel(program, ONLY_ADDRESS_CHUNK_KERNEL, errorCode);
        } else {
            kernel = clCreateKernel(program, ONLY_ADDRESS_NONCHUNK_KERNEL, errorCode);
        }
    }

    private void setBytewisePublicKeyKernel() {
        if (producerOpenCL.chunkMode) {
            kernel = clCreateKernel(program, UNTIL_PUBLIC_KEY_CHUNK_KERNEL, errorCode);
        } else {
            kernel = clCreateKernel(program, UNTIL_PUBLIC_KEY_NONCHUNK_KERNEL, errorCode);
        }
    }

    private void setBytewiseFirstSha256Kernel() {
        if (producerOpenCL.chunkMode) {
            kernel = clCreateKernel(program, UNTIL_1ST_SHA256_CHUNK_KERNEL, errorCode);
        } else {
            kernel = clCreateKernel(program, UNTIL_1ST_SHA256_NONCHUNK_KERNEL, errorCode);
        }
    }

    private void setBytewiseRipemd160Kernel() {
        if (producerOpenCL.chunkMode) {
            kernel = clCreateKernel(program, UNTIL_RIPEMD160_CHUNK_KERNEL, errorCode);
        } else {
            kernel = clCreateKernel(program, UNTIL_RIPEMD160_NONCHUNK_KERNEL, errorCode);
        }
    }

    private void setBytewiseSecondSha256Kernel() {
        if (producerOpenCL.chunkMode) {
            kernel = clCreateKernel(program, UNTIL_2ND_SHA256_CHUNK_KERNEL, errorCode);
        } else {
            kernel = clCreateKernel(program, UNTIL_2ND_SHA256_NONCHUNK_KERNEL, errorCode);
        }
    }

    private void setBytewiseThirdSha256Kernel() {
        if (producerOpenCL.chunkMode) {
            kernel = clCreateKernel(program, UNTIL_3RD_SHA256_CHUNK_KERNEL, errorCode);
        } else {
            kernel = clCreateKernel(program, UNTIL_3RD_SHA256_NONCHUNK_KERNEL, errorCode);
        }
    }

    private void setBytewiseAddressKernel() {
        if (producerOpenCL.chunkMode) {
            kernel = clCreateKernel(program, UNTIL_ADDRESS_CHUNK_KERNEL, errorCode);
        } else {
            kernel = clCreateKernel(program, UNTIL_ADDRESS_NONCHUNK_KERNEL, errorCode);
        }
    }

    public int getErrorCode() {
        return errorCode[0];
    }

    public String getErrorCodeString() {
        return CL.stringFor_errorCode(getErrorCode());
    }

    protected OpenClTask getOpenClTask() {
        return openClTask;
    }

    public void release() {
        openClTask.releaseCl();
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

    /**
     * This method executes the OpenCL kernel to generate publicKeys, SHA-256 hashes, RIPEMD-160 hashes or addresses,
     * depending on the parameter {@link CProducerOpenCL#kernelMode}.
     * <br><br>
     * The parameter {@link CProducerOpenCL#chunkMode} will determine if the given array has to be fully filled
     * with privateKeys or will only need a single one as the first element in the array.
     *
     * @param privateKeys In case of <strong>chunkMode = true</strong> this method only needs
     *                    one privateKey, but in case of <strong>chunkMode = false</strong>
     *                    it needs exactly as many private keys as the work size.
     * @return {@link OpenCLGridResult} containing the set type of result.
     * @throws InvalidWorkSizeException when the number of given privateKeys is not equal the work size.
     */
    public OpenCLGridResult createResult(BigInteger[] privateKeys) throws InvalidWorkSizeException {
        openClTask.setSrcPrivateKeys(privateKeys);
        ByteBuffer dstByteBuffer = openClTask.executeKernel(kernel, commandQueue);
        return new OpenCLGridResult(privateKeys, producerOpenCL.getWorkSize(), dstByteBuffer,
                producerOpenCL.chunkMode, producerOpenCL.kernelMode);
    }

    private static List<String> getResourceNamesContent(List<String> resourceNames) throws IOException {
        List<String> contents = new ArrayList<>();
        for (String resourceName : resourceNames) {
            URL url = Resources.getResource(resourceName);
            String content = Resources.toString(url, StandardCharsets.UTF_8);
            contents.add(content);
        }
        return contents;
    }
}