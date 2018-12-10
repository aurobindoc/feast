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

package feast.core.model;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import feast.core.JobServiceProto.JobServiceTypes.JobDetail;
import feast.core.util.TypeConversion;
import feast.specs.ImportSpecProto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Contains information about a run job. */
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "jobs")
public class JobInfo extends AbstractTimestampEntity {
  // Internal job name. Generated by feast ingestion upon invocation.
  @Id private String id;

  // External job id, generated by the runner and retrieved by feast.
  // Used internally for job management.
  @Column(name = "ext_id")
  private String extId;

  // Import job source type
  @Column(name = "type")
  private String type;

  // Runner type
  @Column(name = "runner")
  private String runner;

  // Job options. Stored as a json string as it is specific to the runner.
  @Column(name = "options")
  private String options;

  // Entities populated by the job
  @ManyToMany
  @JoinTable(
      joinColumns = {@JoinColumn(name = "job_id")},
      inverseJoinColumns = {@JoinColumn(name = "entity_id")})
  private List<EntityInfo> entities;

  // Features populated by the job
  @ManyToMany
  @JoinTable(
      joinColumns = {@JoinColumn(name = "job_id")},
      inverseJoinColumns = {@JoinColumn(name = "feature_id")})
  private List<FeatureInfo> features;

  // Job Metrics
  @OneToMany(mappedBy = "jobInfo",cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Metrics> metrics;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 16)
  private JobStatus status;

  // Raw import spec, stored as a json string.
  @Column(name = "raw", length = 2048)
  private String raw;

  public JobInfo() {
    super();
  }

  public JobInfo(
      String jobId,
      String extId,
      String runner,
      ImportSpecProto.ImportSpec importSpec,
      JobStatus status)
      throws InvalidProtocolBufferException {
    this.id = jobId;
    this.extId = extId;
    this.type = importSpec.getType();
    this.runner = runner;
    this.options = TypeConversion.convertMapToJsonString(importSpec.getOptionsMap());
    this.entities = new ArrayList<>();
    for (String entity : importSpec.getEntitiesList()) {
      EntityInfo entityInfo = new EntityInfo();
      entityInfo.setName(entity);
      this.entities.add(entityInfo);
    }
    this.features = new ArrayList<>();
    for (ImportSpecProto.Field field : importSpec.getSchema().getFieldsList()) {
      if (!field.getFeatureId().equals("")) {
        FeatureInfo featureInfo = new FeatureInfo();
        featureInfo.setId(field.getFeatureId());
        this.features.add(featureInfo);
      }
    }
    this.raw = JsonFormat.printer().print(importSpec);
    this.status = status;
  }

  public JobDetail getJobDetail() {
    return JobDetail.newBuilder()
        .setId(this.id)
        .setExtId(this.extId)
        .setType(this.type)
        .setRunner(this.runner)
        .setStatus(this.status.toString())
        .addAllEntities(
            this.entities.stream().map(EntityInfo::getName).collect(Collectors.toList()))
        .addAllFeatures(
            this.features.stream().map(FeatureInfo::getId).collect(Collectors.toList()))
        .setLastUpdated(TypeConversion.convertTimestamp(this.getLastUpdated()))
        .setCreated(TypeConversion.convertTimestamp(this.getCreated()))
        .build();
  }
}
