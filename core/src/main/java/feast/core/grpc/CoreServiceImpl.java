/*
 * Copyright 2018 The Feast Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package feast.core.grpc;

import com.google.protobuf.Empty;
import com.timgroup.statsd.StatsDClient;
import feast.core.CoreServiceGrpc.CoreServiceImplBase;
import feast.core.CoreServiceProto.CoreServiceTypes.*;
import feast.core.exception.RegistrationException;
import feast.core.exception.RetrievalException;
import feast.core.model.EntityInfo;
import feast.core.model.FeatureGroupInfo;
import feast.core.model.FeatureInfo;
import feast.core.model.StorageInfo;
import feast.core.service.SpecService;
import feast.core.validators.SpecValidator;
import feast.specs.EntitySpecProto.EntitySpec;
import feast.specs.FeatureGroupSpecProto;
import feast.specs.FeatureSpecProto.FeatureSpec;
import feast.specs.StorageSpecProto.StorageSpec;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/** Implementation of the feast core GRPC service. */
@Slf4j
@GRpcService
public class CoreServiceImpl extends CoreServiceImplBase {
  @Autowired private SpecService specService;

  @Autowired private SpecValidator validator;

  @Autowired private StatsDClient statsDClient;

  /**
   * Gets specs for all entities requested in the request. If the retrieval of any one of them
   * fails, the whole request will fail, giving an internal error.
   */
  @Override
  public void getEntities(
      GetEntitiesRequest request, StreamObserver<GetEntitiesResponse> responseObserver) {
    long now = System.currentTimeMillis();
    statsDClient.increment("get_entities_request_count");
    try {
      List<EntitySpec> entitySpecs =
          specService
              .getEntities(request.getIdsList())
              .stream()
              .map(EntityInfo::getEntitySpec)
              .collect(Collectors.toList());
      GetEntitiesResponse response =
          GetEntitiesResponse.newBuilder().addAllEntities(entitySpecs).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      statsDClient.increment("get_entities_request_success");
    } catch (RetrievalException | IllegalArgumentException e) {
      statsDClient.increment("get_entities_request_failed");
      log.error("Error in getEntities: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } finally {
      long duration = System.currentTimeMillis() - now;
      statsDClient.gauge("get_entities_latency_ms", duration);
    }
  }

  /** Gets specs for all entities registered in the registry. */
  @Override
  public void listEntities(Empty request, StreamObserver<ListEntitiesResponse> responseObserver) {
    long now = System.currentTimeMillis();
    statsDClient.increment("list_entities_request_count");
    try {
      List<EntitySpec> entitySpecs =
          specService
              .listEntities()
              .stream()
              .map(EntityInfo::getEntitySpec)
              .collect(Collectors.toList());
      ListEntitiesResponse response =
          ListEntitiesResponse.newBuilder().addAllEntities(entitySpecs).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      statsDClient.increment("list_entities_request_success");
    } catch (RetrievalException e) {
      statsDClient.increment("list_entities_request_failed");
      log.error("Error in listEntities: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } finally {
      long duration = System.currentTimeMillis() - now;
      statsDClient.gauge("list_entities_latency_ms", duration);
    }
  }

  /**
   * Gets specs for all features requested in the request. If the retrieval of any one of them
   * fails, the whole request will fail, giving an internal error.
   */
  @Override
  public void getFeatures(
      GetFeaturesRequest request, StreamObserver<GetFeaturesResponse> responseObserver) {
    long now = System.currentTimeMillis();
    statsDClient.increment("get_features_request_count");
    try {
      List<FeatureSpec> featureSpecs =
          specService
              .getFeatures(request.getIdsList())
              .stream()
              .map(FeatureInfo::getFeatureSpec)
              .collect(Collectors.toList());
      GetFeaturesResponse response =
          GetFeaturesResponse.newBuilder().addAllFeatures(featureSpecs).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      statsDClient.increment("get_features_request_success");
    } catch (RetrievalException | IllegalArgumentException e) {
      statsDClient.increment("get_features_request_failed");
      log.error("Error in getFeatures: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } finally {
      long duration = System.currentTimeMillis() - now;
      statsDClient.gauge("get_features_latency_ms", duration);
    }
  }

  /** Gets specs for all features registered in the registry. TODO: some kind of pagination */
  @Override
  public void listFeatures(Empty request, StreamObserver<ListFeaturesResponse> responseObserver) {
    long now = System.currentTimeMillis();
    statsDClient.increment("list_features_request_count");
    try {
      List<FeatureSpec> featureSpecs =
          specService
              .listFeatures()
              .stream()
              .map(FeatureInfo::getFeatureSpec)
              .collect(Collectors.toList());
      ListFeaturesResponse response =
          ListFeaturesResponse.newBuilder().addAllFeatures(featureSpecs).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      statsDClient.increment("list_features_request_success");
    } catch (RetrievalException e) {
      statsDClient.increment("list_features_request_failed");
      log.error("Error in listFeatures: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } finally {
      long duration = System.currentTimeMillis() - now;
      statsDClient.gauge("list_features_latency_ms", duration);
    }
  }

  /**
   * Gets specs for all storage requested in the request. If the retrieval of any one of them fails,
   * the whole request will fail, giving an internal error.
   */
  @Override
  public void getStorage(
      GetStorageRequest request, StreamObserver<GetStorageResponse> responseObserver) {
    long now = System.currentTimeMillis();
    statsDClient.increment("get_storage_request_count");
    try {
      List<StorageSpec> storageSpecs =
          specService
              .getStorage(request.getIdsList())
              .stream()
              .map(StorageInfo::getStorageSpec)
              .collect(Collectors.toList());
      GetStorageResponse response =
          GetStorageResponse.newBuilder().addAllStorageSpecs(storageSpecs).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      statsDClient.increment("get_storage_request_success");
    } catch (RetrievalException | IllegalArgumentException e) {
      statsDClient.increment("get_storage_request_failed");
      log.error("Error in getStorage: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } finally {
      long duration = System.currentTimeMillis() - now;
      statsDClient.gauge("get_storage_latency_ms", duration);
    }
  }

  /** Gets specs for all storage registered in the registry. */
  @Override
  public void listStorage(Empty request, StreamObserver<ListStorageResponse> responseObserver) {
    long now = System.currentTimeMillis();
    statsDClient.increment("list_storage_request_count");
    try {
      List<StorageSpec> storageSpecs =
          specService
              .listStorage()
              .stream()
              .map(StorageInfo::getStorageSpec)
              .collect(Collectors.toList());
      ListStorageResponse response =
          ListStorageResponse.newBuilder().addAllStorageSpecs(storageSpecs).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      statsDClient.increment("list_storage_request_success");
    } catch (RetrievalException e) {
      statsDClient.increment("list_storage_request_failed");
      log.error("Error in listStorage: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } finally {
      long duration = System.currentTimeMillis() - now;
      statsDClient.gauge("list_storage_latency_ms", duration);
    }
  }

  /**
   * Registers a single feature spec to the registry. If validation fails, will returns a bad
   * request error. If registration fails (e.g. connection to the db is interrupted), an internal
   * error will be returned.
   */
  @Override
  public void registerFeature(
      FeatureSpec request, StreamObserver<RegisterFeatureResponse> responseObserver) {
    try {
      validator.validateFeatureSpec(request);
      FeatureInfo feature = specService.registerFeature(request);
      RegisterFeatureResponse response =
          RegisterFeatureResponse.newBuilder().setFeatureId(feature.getId()).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (RegistrationException e) {
      log.error("Error in registerFeature: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } catch (IllegalArgumentException e) {
      log.error("Error in registerFeature: {}", e);
      responseObserver.onError(getBadRequestException(e));
    }
  }

  /**
   * Registers a single feature group spec to the registry. If validation fails, will returns a bad
   * request error. If registration fails (e.g. connection to the db is interrupted), an internal
   * error will be returned.
   */
  @Override
  public void registerFeatureGroup(
      FeatureGroupSpecProto.FeatureGroupSpec request,
      StreamObserver<RegisterFeatureGroupResponse> responseObserver) {
    try {
      validator.validateFeatureGroupSpec(request);
      FeatureGroupInfo featureGroup = specService.registerFeatureGroup(request);
      RegisterFeatureGroupResponse response =
          RegisterFeatureGroupResponse.newBuilder().setFeatureGroupId(featureGroup.getId()).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (RegistrationException e) {
      log.error("Error in registerFeatureGroup: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } catch (IllegalArgumentException e) {
      log.error("Error in registerFeatureGroup: {}", e);
      responseObserver.onError(getBadRequestException(e));
    }
  }

  /**
   * Registers a single entity spec to the registry. If validation fails, will returns a bad request
   * error. If registration fails (e.g. connection to the db is interrupted), an internal error will
   * be returned.
   */
  @Override
  public void registerEntity(
      EntitySpec request, StreamObserver<RegisterEntityResponse> responseObserver) {
    try {
      validator.validateEntitySpec(request);
      EntityInfo entity = specService.registerEntity(request);
      RegisterEntityResponse response =
          RegisterEntityResponse.newBuilder().setEntityName(entity.getName()).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (RegistrationException e) {
      log.error("Error in registerEntity: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } catch (IllegalArgumentException e) {
      log.error("Error in registerEntity: {}", e);
      responseObserver.onError(getBadRequestException(e));
    }
  }

  /**
   * Registers a single storage to the registry. If validation fails, will returns a bad request
   * error. If registration fails (e.g. connection to the db is interrupted), an internal error will
   * be returned.
   */
  @Override
  public void registerStorage(
      StorageSpec request, StreamObserver<RegisterStorageResponse> responseObserver) {
    try {
      validator.validateStorageSpec(request);
      StorageInfo storage = specService.registerStorage(request);
      RegisterStorageResponse response =
          RegisterStorageResponse.newBuilder().setStorageId(storage.getId()).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (RegistrationException e) {
      log.error("Error in registerStorage: {}", e);
      responseObserver.onError(getRuntimeException(e));
    } catch (IllegalArgumentException e) {
      log.error("Error in registerStorage: {}", e);
      responseObserver.onError(getBadRequestException(e));
    }
  }

  private StatusRuntimeException getRuntimeException(Exception e) {
    return new StatusRuntimeException(
        Status.fromCode(Status.Code.INTERNAL).withDescription(e.getMessage()).withCause(e));
  }

  private StatusRuntimeException getBadRequestException(Exception e) {
    return new StatusRuntimeException(
        Status.fromCode(Status.Code.OUT_OF_RANGE).withDescription(e.getMessage()).withCause(e));
  }
}
