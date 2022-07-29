#!/bin/bash

PAYARA_KIND=MICRO

logback="-Dlogback.configurationFile=/opt/payara5/scripts/logback.xml -Dfish.payara.classloading.delegate=false -DlogbackDisableServletContainerInitializer=true"
hazelcast_phone_home="-Dhazelcast.phone.home.enabled=false"
log_properties="--logProperties /opt/payara5/scripts/logging.properties"
openapi_servers=""
if [[ -v MP_OPENAPI_SERVERS ]]; then
  openapi_servers="-Dmp.openapi.servers=${MP_OPENAPI_SERVERS}"
fi
pre_boot_commands="--prebootcommandfile ./scripts/prebootcommandfile.txt"
post_boot_commands="--postbootcommandfile ./scripts/postbootcommandfile.txt"
config_root_dir=/opt/payara5/microRootDir
hazelcast_config="--hzconfigfile ${config_root_dir}/config/hazelcast.xml"
# needs to be visible in logback.xml
export PAYARA_CONFIG_DIR=${config_root_dir}/config
DEPLOY_DIR=/opt/payara5/deployments
PRE_BOOT_COMMAND_FILE=./scripts/prebootcommandfile.txt
POST_BOOT_COMMAND_FILE=./scripts/postbootcommandfile.txt

function get_payara_micro_cmd_line() {
    echo /usr/bin/java -Djdk.util.jar.enableMultiRelease=force $(enable_remote_debugging) ${logback} ${hazelcast_phone_home} ${openapi_servers} $(get_jvm_max_memory_options) $(get_jvm_extra_options) -cp ./payara-micro.jar:$(get_jars) fish.payara.micro.PayaraMicro ${pre_boot_commands} ${post_boot_commands} --addjars $(get_jars) --port 8080  ${log_properties} --deploydir ${DEPLOY_DIR} --disablephonehome --rootdir ${config_root_dir} ${hazelcast_config}
}

function get_jars() {
   if [ ! -d jars ] ; then
       return;
   fi

   local seperator=""
   local result=""
   for file in $(ls jars) ; do
       if [ ${file: -4} == ".jar" ] ; then
           result=${result}${seperator}jars/${file}
           seperator=":"
       fi
   done
   echo ${result}
}

rm -rf ${config_root_dir}
mkdir -p ${PAYARA_CONFIG_DIR}

source /opt/payara5/scripts/common-start-payara.bash

function get_jvm_max_memory_options() {
   echo " -Xms1g -Xmx${_PAYARA_MAX_HEAP} -XshowSettings:vm"
}

function enable_remote_debugging() {
    # compare using lowercase value of ENABLE_REMOTE_DEBUGGING
    test -n "${REMOTE_DEBUGGING_HOST}" && echo "-agentlib:jdwp=transport=dt_socket,server=n,address=${REMOTE_DEBUGGING_HOST},suspend=y"
}


info "$(get_payara_micro_cmd_line)"
$(get_payara_micro_cmd_line) &
payara_pid=$!
info "payara pid: $payara_pid, bash pid: $BASHPID"

# When 1st wait is interrupted by signal, it returns immediately with code 128 plus the numeric value of the signal,
# 2nd wait ensures that we actually wait for payara to shutdown
wait $payara_pid
wait $payara_pid
payara_exitcode=$?

info "payara($payara_pid) shutdown complete with exit code $payara_exitcode"

exit $payara_exitcode