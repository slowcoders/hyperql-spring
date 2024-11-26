package org.slowcoders.hyperql.jpa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.slowcoders.hyperql.util.ClassUtils;

import java.io.IOException;
import java.util.List;

/**
 * Serialize 시 무한 루프 방지를 위한 Jackson 모듈
 * 참고) @JsonBackReference 와 @JsonIdentityInfo 대체
 */
public class HyperJpaModule extends SimpleModule {

    public HyperJpaModule() {
    }
    @Override
    public String getModuleName() {
        return "JQLJackson";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addBeanSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                             BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                Class<?> clazz = beanDesc.getBeanClass();
                if (JpaUtils.isJpaEntityType(clazz)) {
                    for (int i = 0; i < beanProperties.size(); i++) {
                        BeanPropertyWriter writer = beanProperties.get(i);
                        beanProperties.set(i, new JpaPropertyFilter(writer));
                    }
                }
                return beanProperties;
            }
        });

        Class pgClass = ClassUtils.findClassOrNull("org.postgresql.util.PGobject");
        if (pgClass != null) {
            SimpleSerializers serializers = new SimpleSerializers();

            serializers.addSerializer(pgClass, new JsonSerializer<Object>() {
                @Override
                public void serialize(Object pgObj, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
                    gen.writeRawValue(pgObj.toString());
                }
            });
            context.addSerializers(serializers);
        }
    }

}