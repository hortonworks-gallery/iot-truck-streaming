package com.hortonworks.streaming.impl.bolts;

import org.apache.storm.tuple.Tuple;


public class Prediction {

  private Double _prediction;
  private Tuple _event;
  private double[] _predictionParams;

  public Prediction(Double prediction, Tuple event, double[] predictionParams) {
    _prediction = prediction;
    _event = event;
    _predictionParams = predictionParams;
  }


  public Double getPrediction() {
    return _prediction;
  }

  public void setPrediction(Double prediction) {
    this._prediction = prediction;
  }

  public Tuple get_event() {
    return _event;
  }


  public void set_event(Tuple _event) {
    this._event = _event;
  }


  public double[] get_predictionParams() {
    return _predictionParams;
  }


  public void set_predictionParams(double[] _predictionParams) {
    this._predictionParams = _predictionParams;
  }


}

