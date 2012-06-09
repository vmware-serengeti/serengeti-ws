#!/bin/bash

SERENGETI_HOME="/opt/serengeti"
SERENGETI_SCRIPTS_HOME="${SERENGETI_HOME}/sbin"
SERENGETI_ETC_HOME="${SERENGETI_HOME}/etc"
SERENGETI_LOG="${SERENGETI_HOME}/logs"
SERENGETI_LOG_INSTALLER="${SERENGETI_LOG}/serengeti-firstboot.log"
SERENGETI_ERR_INSTALLER="${SERENGETI_LOG}/serengeti-firstboot.err"
SERENGETI_LOG_UPDATE="${SERENGETI_LOG}/serengeti-subsequentboot.log"
SERENGETI_ERR_UPDATE="${SERENGETI_LOG}/serengeti-subsequentboot.err"

mkdir -p "${SERENGETI_LOG}"

if [ ! -e "${SERENGETI_ETC_HOME}/serengeti-firstboot" ]
then
    touch "${SERENGETI_LOG_INSTALLER}"
    echo `date` >> ${SERENGETI_LOG_INSTALLER}
    echo `date` >> ${SERENGETI_ERR_INSTALLER}
    chmod +x "${SERENGETI_SCRIPTS_HOME}/serengeti-firstboot.rb"
    /usr/bin/ruby "${SERENGETI_SCRIPTS_HOME}/serengeti-firstboot.rb" 1>>${SERENGETI_LOG_INSTALLER} 2>>${SERENGETI_ERR_INSTALLER}
    touch "${SERENGETI_ETC_HOME}/serengeti-firstboot"
else
    touch "${SERENGETI_LOG_UPDATE}"
    echo `date` >> ${SERENGETI_LOG_UPDATE}
    echo `date` >> ${SERENGETI_ERR_UPDATE}
    chmod +x "${SERENGETI_SCRIPTS_HOME}/serengeti-subsequentboot.rb"
    /usr/bin/ruby "${SERENGETI_SCRIPTS_HOME}/serengeti-subsequentboot.rb" 1>>${SERENGETI_LOG_UPDATE} 2>>${SERENGETI_ERR_UPDATE}
fi