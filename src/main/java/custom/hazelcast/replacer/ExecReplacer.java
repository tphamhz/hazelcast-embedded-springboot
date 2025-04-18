
/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package custom.hazelcast.replacer;

 import com.hazelcast.config.replacer.spi.ConfigReplacer;
 import com.hazelcast.logging.ILogger;
 import com.hazelcast.logging.Logger;
 
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.nio.charset.StandardCharsets;
 import java.util.Arrays;
 import java.util.Properties;
 
 import static com.hazelcast.internal.nio.IOUtil.closeResource;
 import static com.hazelcast.internal.util.StringUtil.isNullOrEmpty;
 
 /**
  * This class is an <b>example</b> {@link ConfigReplacer} implementation which allows to replace variables with a standard
  * output of an executed command.
  * <p>
  * <i><b>Warning:</b> This replacer can be used to execute arbitrary commands as the hazelcast instance during service
  * initialization.</i>
  * <p>
  * The prefix of this replacer is {@value #PREFIX} and the full format of variables is
  * <code>$EXEC{executable arg1 arg2 ...}</code> (e.g. <code>$EXEC{echo Hello}</code>).
  * <p>
  * By default whitespaces are used as argument separators. If some argument value contains a whitespace, then configure another
  * argument separator in {@value #PROPERTY_ARGUMENT_SEPARATOR} init property.
  */
 public class ExecReplacer implements ConfigReplacer {
 
     /**
      * Replacer property name to configure argument separator regular expression.
      */
     public static final String PROPERTY_ARGUMENT_SEPARATOR = "argumentSeparator";
     /**
      * Replacer property name which controls if a {@code exitCode==0} is required to successfully finish the replacement.
      */
     public static final String PROPERTY_REQUIRES_ZERO_EXIT = "requiresZeroExitCode";
     /**
      * Default value for property {@link #PROPERTY_REQUIRES_ZERO_EXIT}.
      */
     public static final boolean DEFAULT_REQUIRES_ZERO_EXIT = true;
 
     private static final String PREFIX = "EXEC";
     private static final ILogger LOGGER = Logger.getLogger(ExecReplacer.class);
 
     private String argumentSeparator;
     private boolean requiresZeroExitCode;
 
     @Override
     public void init(Properties properties) {
         argumentSeparator = properties.getProperty(PROPERTY_ARGUMENT_SEPARATOR, "\\s+");
         String property = properties.getProperty(PROPERTY_REQUIRES_ZERO_EXIT);
         requiresZeroExitCode = isNullOrEmpty(property) ? DEFAULT_REQUIRES_ZERO_EXIT : Boolean.parseBoolean(property);
     }
 
     @Override
     public String getPrefix() {
         return PREFIX;
     }
 
     @Override
     public String getReplacement(String variable) {
         try {
             return processWaitAndGetOutput(variable);
         } catch (Exception e) {
             LOGGER.warning("Execution failed for variable " + variable, e);
         }
         return null;
     }
 
     /**
      * Waits for termination of the given process and returns Standard output
      */
     private String processWaitAndGetOutput(String command) throws InterruptedException, IOException {
         String[] cmdarray = command.split(argumentSeparator);
         LOGGER.info("Executing command " + Arrays.toString(cmdarray));
         Process process = Runtime.getRuntime().exec(cmdarray);
         StdStreamReader outReader = new StdStreamReader(process.getInputStream(), true);
         outReader.start();
         StdStreamReader errReader = new StdStreamReader(process.getErrorStream(), false);
         errReader.start();
 
         process.waitFor();
         outReader.join();
         errReader.join();
         int exitCode = process.exitValue();
 
         if (exitCode != 0) {
             LOGGER.warning("Command finished with non-zero exit code (" + exitCode + "): " + Arrays.toString(cmdarray));
             if (requiresZeroExitCode) {
                 return null;
             }
         }
 
         return outReader.getStreamContent();
     }
 
     /**
      * Reader thread for process streams. It prevents SIGPIPE related process termination. It has 2 modes:<br>
      * i) copy standard stream to a byte array and provide it as String<br>
      * ii) just consume the bytes from the stream
      */
     private static class StdStreamReader extends Thread {
 
         private static final int BUFFER_SIZE = 1024;
 
         private final InputStream is;
         private final boolean copy;
         private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
 
         StdStreamReader(InputStream is, boolean copy) {
             this.is = is;
             this.copy = copy;
         }
 
         @Override
         public void run() {
             try {
                 byte[] buffer = new byte[BUFFER_SIZE];
                 int n;
                 while (-1 != (n = is.read(buffer))) {
                     if (copy) {
                         baos.write(buffer, 0, n);
                     }
                 }
             } catch (IOException e) {
                 LOGGER.warning("Reading a standard stream failed.", e);
             } finally {
                 closeResource(is);
             }
         }
 
         /**
          * Returns all read bytes as String (with UTF-8 charset used).
          */
         public String getStreamContent() {
             return baos.toString(StandardCharsets.UTF_8);
         }
     }
 }