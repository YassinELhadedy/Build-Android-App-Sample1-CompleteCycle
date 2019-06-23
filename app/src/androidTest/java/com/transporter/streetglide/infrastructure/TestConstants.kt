package com.transporter.streetglide.infrastructure

/**
 * TestConstants
 */
/*
 * FIXME: The IP of the VM should be dynamic. We can get it using:
 *        arp -an | grep $(virsh dumpxml superglide | tr -d '\n' | grep  '<mac' | sed "s/^.*<mac[^']*'//" | sed "s/'.*$//") | sed "s/^[^(]*(//" | sed "s/).*$//"
 */
const val IP_ADD = "172.17.8.102"
const val MOCK_BASE_URL = "https://$IP_ADD/api/v1/"
const val USER_NAME = "omar"
const val PASS = "adel1234"
const val KEY_PREFERENCE = "streetglide.preference"