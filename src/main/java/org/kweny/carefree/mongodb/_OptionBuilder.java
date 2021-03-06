/*
 * Copyright (C) 2018 Kweny.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kweny.carefree.mongodb;

import com.mongodb.*;
import com.mongodb.event.*;
import com.mongodb.selector.ServerSelector;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 根据 {@link MongoCarefreeStructure} 的配置创建 {@link MongoClientOptions.Builder}。
 *
 * @author Kweny
 * @since 1.0.0
 */
class _OptionBuilder {

    // w1 / w2 / w10 ...
    private static final Pattern PATTERN_WX = Pattern.compile("w(\\d+)");
    // w2-10000-true / w2-10000-false
    private static final Pattern PATTERN_WX_TIMEOUT_JOURNAL = Pattern.compile("w(\\d+)-(\\d+)-([a-zA-Z]+)");
    // secondary-[{a=0,b=1},{c=3,d=4},{e=5,f=6}]-10000 / secondary-[{a=0,b=1}] / secondary-10000 / secondary
    private static final Pattern PATTERN_READ_PREFERENCE = Pattern.compile("([a-zA-Z]+)(-\\[(\\{[a-zA-Z=0-9,]+}(,\\{[a-zA-Z=0-9,]+})*)?])?(-(\\d+))?");

    static MongoClientOptions.Builder buildMongoClientOptions(MongoCarefreeStructure structure) {
        MongoClientOptions.Builder builder = MongoClientOptions.builder();

        if (structure.getDescription() != null) {
            builder.description(structure.getDescription());
        }
        if (structure.getApplicationName() != null) {
            builder.applicationName(structure.getApplicationName());
        }

        if (structure.getConnectTimeout() != null) {
            builder.connectTimeout(structure.getConnectTimeout());
        }
        if (structure.getSocketTimeout() != null) {
            builder.socketTimeout(structure.getSocketTimeout());
        }

        if (structure.getMaxWaitTime() != null) {
            builder.maxWaitTime(structure.getMaxWaitTime());
        }
        if (structure.getMinConnectionsPerHost() != null) {
            builder.minConnectionsPerHost(structure.getMinConnectionsPerHost());
        }
        if (structure.getMaxConnectionsPerHost() != null) {
            builder.connectionsPerHost(structure.getMaxConnectionsPerHost());
        }
        if (structure.getMaxConnectionIdleTime() != null) {
            builder.maxConnectionIdleTime(structure.getMaxConnectionIdleTime());
        }
        if (structure.getMaxConnectionLifeTime() != null) {
            builder.maxConnectionLifeTime(structure.getMaxConnectionLifeTime());
        }
        if (structure.getThreadsAllowedToBlockForConnectionMultiplier() != null) {
            builder.threadsAllowedToBlockForConnectionMultiplier(structure.getThreadsAllowedToBlockForConnectionMultiplier());
        }

        if (structure.getRetryWrites() != null) {
            builder.retryWrites(structure.getRetryWrites());
        }
        if (structure.getAlwaysUseMBeans() != null) {
            builder.alwaysUseMBeans(structure.getAlwaysUseMBeans());
        }

        if (structure.getSslEnabled() != null) {
            builder.sslEnabled(structure.getSslEnabled());
        }
        if (structure.getSslInvalidHostNameAllowed() != null) {
            builder.sslInvalidHostNameAllowed(structure.getSslInvalidHostNameAllowed());
        }

        if (structure.getLocalThreshold() != null) {
            builder.localThreshold(structure.getLocalThreshold());
        }
        if (structure.getServerSelectionTimeout() != null) {
            builder.serverSelectionTimeout(structure.getServerSelectionTimeout());
        }
        if (structure.getServerSelector() != null) {
            try {
                Class<?> clazz = Class.forName(structure.getServerSelector());
                Object serverSelector = clazz.getConstructor().newInstance();
                builder.serverSelector((ServerSelector) serverSelector);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (structure.getRequiredReplicaSetName() != null) {
            builder.requiredReplicaSetName(structure.getRequiredReplicaSetName());
        }

        if (structure.getWriteConcern() != null) {
            WriteConcern writeConcern = resolveWriteConcern(structure.getWriteConcern());
            if (writeConcern != null) {
                builder.writeConcern(writeConcern);
            }
        }

        if (structure.getReadConcern() != null) {
            ReadConcern readConcern = resolveReadConcern(structure.getReadConcern());
            if (readConcern != null) {
                builder.readConcern(readConcern);
            }
        }

        if (structure.getReadPreference() != null) {
            ReadPreference readPreference = resolveReadPreference(structure.getReadConcern());
            if (readPreference != null) {
                builder.readPreference(readPreference);
            }
        }

        resolveListeners(builder, structure.getCommandListeners(), ListenerType.COMMAND);
        resolveListeners(builder, structure.getClusterListeners(), ListenerType.CLUSTER);
        resolveListeners(builder, structure.getConnectionPoolListeners(), ListenerType.CONNECTION_POOL);
        resolveListeners(builder, structure.getServerListeners(), ListenerType.SERVER);
        resolveListeners(builder, structure.getServerMonitorListeners(), ListenerType.SERVER_MONITOR);

        List<MongoCarefreeOptionedListener> optionedListeners = resolveOptionedListeners(structure.getOptionedListeners());
        if (optionedListeners != null && optionedListeners.size() > 0) {
            for (MongoCarefreeOptionedListener optionedListener : optionedListeners) {
                optionedListener.optioned(structure, builder);
            }
        }

        return builder;
    }

    private static WriteConcern resolveWriteConcern(String propertyValue) {
        if ("w0".equalsIgnoreCase(propertyValue)) {
            return WriteConcern.UNACKNOWLEDGED;
        } else if ("w1".equalsIgnoreCase(propertyValue)) {
            return WriteConcern.W1;
        } else if ("w2".equalsIgnoreCase(propertyValue)) {
            return WriteConcern.W2;
        } else if ("w3".equalsIgnoreCase(propertyValue)) {
            return WriteConcern.W3;
        } else if ("majority".equalsIgnoreCase(propertyValue)) {
            return WriteConcern.MAJORITY;
        } else if ("journal".equalsIgnoreCase(propertyValue)) {
            return WriteConcern.JOURNALED;
        } else {
            String culledPropertyValue = _Assistant.deleteWhitespace(propertyValue);
            if (PATTERN_WX.matcher(culledPropertyValue.toLowerCase()).matches()) {
                String wStr = culledPropertyValue.toLowerCase().replaceAll(PATTERN_WX.pattern(), "$1");
                return new WriteConcern(Integer.parseInt(wStr));
            } else if (PATTERN_WX_TIMEOUT_JOURNAL.matcher(culledPropertyValue.toLowerCase()).matches()) {
                String wStr = culledPropertyValue.toLowerCase().replaceAll(PATTERN_WX_TIMEOUT_JOURNAL.pattern(), "$1");
                String wTimeoutStr = culledPropertyValue.toLowerCase().replaceAll(PATTERN_WX_TIMEOUT_JOURNAL.pattern(), "$2");
                String journalStr = culledPropertyValue.toLowerCase().replaceAll(PATTERN_WX_TIMEOUT_JOURNAL.pattern(), "$3");
                int w = Integer.parseInt(wStr);
                int wTimeout = Integer.parseInt(wTimeoutStr);
                boolean journal = Boolean.parseBoolean(journalStr);
                return new WriteConcern(w).withWTimeout(wTimeout, TimeUnit.MILLISECONDS).withJournal(journal);
            }
        }
        return null;
    }

    private static ReadConcern resolveReadConcern(String propertyValue) {
        if ("local".equalsIgnoreCase(propertyValue)) {
            return ReadConcern.LOCAL;
        } else if ("majority".equalsIgnoreCase(propertyValue)) {
            return ReadConcern.MAJORITY;
        } else if ("linearizable".equalsIgnoreCase(propertyValue)) {
            return ReadConcern.LINEARIZABLE;
        } else if ("snapshot".equalsIgnoreCase(propertyValue)) {
            return ReadConcern.SNAPSHOT;
        } else {
            return null;
        }
    }

    private static ReadPreference resolveReadPreference(String propertyValue) {
        if ("primary".equalsIgnoreCase(propertyValue)) {
            return ReadPreference.primary();
        } else if ("primaryPreferred".equalsIgnoreCase(propertyValue)) {
            return ReadPreference.primaryPreferred();
        } else if ("secondary".equalsIgnoreCase(propertyValue)) {
            return ReadPreference.secondary();
        } else if ("secondaryPreferred".equalsIgnoreCase(propertyValue)) {
            return ReadPreference.secondaryPreferred();
        } else if ("nearest".equalsIgnoreCase(propertyValue)) {
            return ReadPreference.nearest();
        } else {
            String culledPropertyValue = _Assistant.deleteWhitespace(propertyValue);
            if (PATTERN_READ_PREFERENCE.matcher(culledPropertyValue).matches()) {
                String mode = culledPropertyValue.replaceAll(PATTERN_READ_PREFERENCE.pattern(), "$1");
                String tagSetListStr = culledPropertyValue.replaceAll(PATTERN_READ_PREFERENCE.pattern(), "$3");
                String stalenessStr = culledPropertyValue.replaceAll(PATTERN_READ_PREFERENCE.pattern(), "$6");

                List<TagSet> tagSetList = new ArrayList<>();
                if (tagSetListStr.trim().length() > 0) {
                    String[] tagSetElements = tagSetListStr.split(",");
                    for (String tagSetElement : tagSetElements) {
                        tagSetElement = tagSetElement.substring(1, tagSetElement.length() - 1);

                        List<Tag> tagList = new ArrayList<>();
                        String[] tagElements = tagSetElement.split(",");
                        for (String tagElement : tagElements) {
                            String[] parts = tagElement.split("=");
                            if (parts.length != 2) {
                                throw new IllegalArgumentException("Invalid readPreference value: " + propertyValue);
                            }
                            Tag tag = new Tag(parts[0], parts[1]);
                            tagList.add(tag);
                        }

                        if (tagList.size() > 0) {
                            TagSet tagSet = new TagSet(tagList);
                            tagSetList.add(tagSet);
                        }
                    }
                }

                Integer maxStaleness = null;
                if (stalenessStr.trim().length() > 0) {
                    maxStaleness = Integer.parseInt(stalenessStr.trim());
                }

                if ("primary".equalsIgnoreCase(mode)) {
                    return ReadPreference.primary();
                } else if ("primaryPreferred".equalsIgnoreCase(mode)) {
                    if (tagSetList.size() > 0 && maxStaleness != null) {
                        return ReadPreference.primaryPreferred(tagSetList, maxStaleness, TimeUnit.MILLISECONDS);
                    } else if (tagSetList.size() > 0) {
                        return ReadPreference.primaryPreferred(tagSetList);
                    } else if (maxStaleness != null) {
                        return ReadPreference.primaryPreferred(maxStaleness, TimeUnit.MILLISECONDS);
                    } else {
                        return ReadPreference.primaryPreferred();
                    }
                } else if ("secondary".equalsIgnoreCase(mode)) {
                    if (tagSetList.size() > 0 && maxStaleness != null) {
                        return ReadPreference.secondary(tagSetList, maxStaleness, TimeUnit.MILLISECONDS);
                    } else if (tagSetList.size() > 0) {
                        return ReadPreference.secondary(tagSetList);
                    } else if (maxStaleness != null) {
                        return ReadPreference.secondary(maxStaleness, TimeUnit.MILLISECONDS);
                    } else {
                        return ReadPreference.secondary();
                    }
                } else if ("secondaryPreferred".equalsIgnoreCase(mode)) {
                    if (tagSetList.size() > 0 && maxStaleness != null) {
                        return ReadPreference.secondaryPreferred(tagSetList, maxStaleness, TimeUnit.MILLISECONDS);
                    } else if (tagSetList.size() > 0) {
                        return ReadPreference.secondaryPreferred(tagSetList);
                    } else if (maxStaleness != null) {
                        return ReadPreference.secondaryPreferred(maxStaleness, TimeUnit.MILLISECONDS);
                    } else {
                        return ReadPreference.secondaryPreferred();
                    }
                } else if ("nearest".equalsIgnoreCase(mode)) {
                    if (tagSetList.size() > 0 && maxStaleness != null) {
                        return ReadPreference.nearest(tagSetList, maxStaleness, TimeUnit.MILLISECONDS);
                    } else if (tagSetList.size() > 0) {
                        return ReadPreference.nearest(tagSetList);
                    } else if (maxStaleness != null) {
                        return ReadPreference.nearest(maxStaleness, TimeUnit.MILLISECONDS);
                    } else {
                        return ReadPreference.nearest();
                    }
                }
            }
        }

        return null;
    }

    private enum ListenerType {
        COMMAND, CLUSTER, CONNECTION_POOL, SERVER, SERVER_MONITOR
    }

    private static void resolveListeners(MongoClientOptions.Builder builder, List<String> classNames, ListenerType type) {
        if (classNames == null || classNames.size() == 0) {
            return;
        }
        for (String className : classNames) {
            try {
                Object listener = Class.forName(className).getConstructor().newInstance();
                switch (type) {
                    case COMMAND:
                        builder.addCommandListener((CommandListener) listener);
                        break;
                    case CLUSTER:
                        builder.addClusterListener((ClusterListener) listener);
                        break;
                    case CONNECTION_POOL:
                        builder.addConnectionPoolListener((ConnectionPoolListener) listener);
                        break;
                    case SERVER:
                        builder.addServerListener((ServerListener) listener);
                        break;
                    case SERVER_MONITOR:
                        builder.addServerMonitorListener((ServerMonitorListener) listener);
                        break;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static List<MongoCarefreeOptionedListener> resolveOptionedListeners(List<String> classNames) {
        if (classNames == null || classNames.size() == 0) {
            return null;
        }
        List<MongoCarefreeOptionedListener> listeners = new LinkedList<>();
        for (String className : classNames) {
            try {
                Object listener = Class.forName(className).getConstructor().newInstance();
                listeners.add((MongoCarefreeOptionedListener) listener);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return listeners;
    }
}