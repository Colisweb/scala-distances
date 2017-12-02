package com.guizmaii.distances.utils

import monix.execution.ExecutionModel
import monix.execution.schedulers.SchedulerService

private[distances] object MonixSchedulers {

  object AlwaysAsyncForkJoinScheduler {
    implicit val scheduler: SchedulerService =
      monix.execution.Scheduler.forkJoin(
        Runtime.getRuntime.availableProcessors(),
        256,
        executionModel = ExecutionModel.AlwaysAsyncExecution
      )
  }

}
