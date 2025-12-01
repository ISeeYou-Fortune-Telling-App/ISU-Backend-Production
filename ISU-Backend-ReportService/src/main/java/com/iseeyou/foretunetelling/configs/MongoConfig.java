package com.iseeyou.foretunetelling.configs;

import com.iseeyou.foretunetelling.utils.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new SeerTierReadConverter(),
                new SeerTierWriteConverter(),
                new CustomerTierReadConverter(),
                new CustomerTierWriteConverter()
        ));
    }

    /**
     * Converter to read SeerTier from MongoDB (from Integer)
     * Handles ordinal values (0,1,2,3)
     */
    @ReadingConverter
    static class SeerTierReadConverter implements Converter<Integer, Constants.SeerTier> {
        @Override
        public Constants.SeerTier convert(Integer source) {
            Constants.SeerTier[] values = Constants.SeerTier.values();
            if (source >= 0 && source < values.length) {
                return values[source];
            }
            throw new IllegalArgumentException("Invalid SeerTier ordinal: " + source + ". Valid range: 0-" + (values.length - 1));
        }
    }

    /**
     * Converter to read CustomerTier from MongoDB (from Integer)
     * Handles ordinal values (0,1,2,3)
     */
    @ReadingConverter
    static class CustomerTierReadConverter implements Converter<Integer, Constants.CustomerTier> {
        @Override
        public Constants.CustomerTier convert(Integer source) {
            Constants.CustomerTier[] values = Constants.CustomerTier.values();
            if (source >= 0 && source < values.length) {
                return values[source];
            }
            throw new IllegalArgumentException("Invalid CustomerTier ordinal: " + source + ". Valid range: 0-" + (values.length - 1));
        }
    }

    /**
     * Converter to write SeerTier to MongoDB as integer ordinal
     */
    @WritingConverter
    static class SeerTierWriteConverter implements Converter<Constants.SeerTier, Integer> {
        @Override
        public Integer convert(Constants.SeerTier source) {
            return source.ordinal();
        }
    }

    /**
     * Converter to write CustomerTier to MongoDB as integer ordinal
     */
    @WritingConverter
    static class CustomerTierWriteConverter implements Converter<Constants.CustomerTier, Integer> {
        @Override
        public Integer convert(Constants.CustomerTier source) {
            return source.ordinal();
        }
    }
}

