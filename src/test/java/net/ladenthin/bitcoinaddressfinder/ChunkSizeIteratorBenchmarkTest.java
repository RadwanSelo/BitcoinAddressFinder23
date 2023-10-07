package net.ladenthin.bitcoinaddressfinder;

import net.ladenthin.bitcoinaddressfinder.benchmark.BenchmarkFactory;
import net.ladenthin.bitcoinaddressfinder.benchmark.BenchmarkFactoryException;
import net.ladenthin.bitcoinaddressfinder.benchmark.types.BenchmarkType;
import net.ladenthin.bitcoinaddressfinder.configuration.CBenchmark;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ChunkSizeIteratorBenchmarkTest {

    @Test
    public void test_start_untilGridNumBits1_3totalResults() throws BenchmarkFactoryException {
        // arrange
        CBenchmark configFile = new CBenchmark();
        configFile.type = BenchmarkFactory.TYPE_CHUNK_ITERATOR;
        configFile.gridNumBits = 1;
        configFile.chunkMode = true;
        configFile.kernelMode = 2;
        configFile.contextRounds = 1;
        configFile.logToConsole = true;
        configFile.logToFile = false;
        BenchmarkFactory factory = new BenchmarkFactory(configFile);
        BenchmarkType runner = factory.createBenchmarkRunner();

        // act
        runner.start();

        // assert
        assertThat(runner.getTotalNumberOfResults(), equalTo(3));
    }

    @Test
    public void test_start_untilGridNumBits8_5110totalResults() throws BenchmarkFactoryException {
        // arrange
        CBenchmark configFile = new CBenchmark();
        configFile.type = BenchmarkFactory.TYPE_CHUNK_ITERATOR;
        configFile.gridNumBits = 8;
        configFile.chunkMode = true;
        configFile.kernelMode = 2;
        configFile.contextRounds = 10;
        configFile.logToConsole = true;
        configFile.logToFile = false;
        BenchmarkFactory factory = new BenchmarkFactory(configFile);
        BenchmarkType runner = factory.createBenchmarkRunner();

        // act
        runner.start();

        // assert
        assertThat(runner.getTotalNumberOfResults(), equalTo(5110));
    }
}