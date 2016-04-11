package org.qcri.rheem.profiler.spark;

import org.apache.spark.api.java.JavaRDD;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.profiler.util.ProfilingUtils;
import org.qcri.rheem.spark.channels.ChannelExecutor;
import org.qcri.rheem.spark.operators.SparkExecutionOperator;

import java.util.function.Supplier;

/**
 * {@link SparkOperatorProfiler} implementation for {@link SparkExecutionOperator}s with one input and no outputs.
 */
public class SinkProfiler extends SparkOperatorProfiler {

    private JavaRDD<?> inputRdd;

    public SinkProfiler(Supplier<SparkExecutionOperator> operatorGenerator,
                        Configuration configuration,
                        Supplier<?> dataQuantumGenerator) {
        super(operatorGenerator, configuration, dataQuantumGenerator);
    }

    @Override
    protected void prepareInput(int inputIndex, long inputCardinality) {
        assert inputIndex == 0;
        this.inputRdd = this.prepareInputRdd(inputCardinality, inputIndex);
    }

    @Override
    protected Result executeOperator() {
        final ChannelExecutor inputChannelExecutor = createChannelExecutor(this.inputRdd, this.sparkExecutor);

        // Let the operator execute.
        ProfilingUtils.sleep(this.executionPaddingTime); // Pad measurement with some idle time.
        final long startTime = System.currentTimeMillis();
        this.operator.evaluate(
                new ChannelExecutor[] { inputChannelExecutor },
                new ChannelExecutor[] {  },
                this.functionCompiler,
                this.sparkExecutor
        );

        // Complete the measurement.
        final long endTime = System.currentTimeMillis();
        ProfilingUtils.sleep(this.executionPaddingTime); // Pad measurement with some idle time.


        // Gather and assemble all result metrics.
        return new Result(
                this.inputCardinalities,
                0,
                endTime - startTime,
                this.provideDiskBytes(startTime, endTime),
                this.provideNetworkBytes(startTime, endTime),
                this.provideCpuCycles(startTime, endTime),
                this.numMachines,
                this.numCoresPerMachine
        );
    }
}
