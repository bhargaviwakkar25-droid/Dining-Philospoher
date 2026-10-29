package diningphilosopher;

import java.util.Random;

public class NeuralNetwork {

    private final int inputDim;
    private final int hiddenDim;
    private final int outputDim;
    private double[][] w1;
    private double[] b1;

    private double[][] w2;
    private double[] b2;

    private final Random random = new Random(42);

    public NeuralNetwork(int inputDim, int hiddenDim, int outputDim) {
        this.inputDim = inputDim;
        this.hiddenDim = hiddenDim;
        this.outputDim = outputDim;

        this.w1 = new double[hiddenDim][inputDim];
        this.b1 = new double[hiddenDim];
        this.w2 = new double[outputDim][hiddenDim];
        this.b2 = new double[outputDim];

        initializeWeights();
    }

    private void initializeWeights() {
        double std1 = Math.sqrt(2.0 / inputDim);
        for (int i = 0; i < hiddenDim; i++) {
            for (int j = 0; j < inputDim; j++) {
                w1[i][j] = random.nextGaussian() * std1;
            }
            b1[i] = 0.0;
        }

        double std2 = Math.sqrt(2.0 / hiddenDim);
        for (int i = 0; i < outputDim; i++) {
            for (int j = 0; j < hiddenDim; j++) {
                w2[i][j] = random.nextGaussian() * std2;
            }
            b2[i] = 0.0;
        }
    }

    public double[] forward(double[] input) {
        // Hidden layer pre-activations and activations
        double[] hidden = new double[hiddenDim];
        for (int i = 0; i < hiddenDim; i++) {
            double sum = b1[i];
            for (int j = 0; j < inputDim; j++) {
                sum += w1[i][j] * input[j];
            }
            hidden[i] = Math.max(0.0, sum); // ReLU activation
        }

        // Output layer (linear activation for Q-values)
        double[] output = new double[outputDim];
        for (int i = 0; i < outputDim; i++) {
            double sum = b2[i];
            for (int j = 0; j < hiddenDim; j++) {
                sum += w2[i][j] * hidden[j];
            }
            output[i] = sum;
        }

        return output;
    }

    public synchronized double trainStep(double[] input, int action, double target, double learningRate) {
        // Forward pass with saved activations
        double[] z1 = new double[hiddenDim];
        double[] h1 = new double[hiddenDim];
        for (int i = 0; i < hiddenDim; i++) {
            double sum = b1[i];
            for (int j = 0; j < inputDim; j++) {
                sum += w1[i][j] * input[j];
            }
            z1[i] = sum;
            h1[i] = Math.max(0.0, sum);
        }

        double predQ = b2[action];
        for (int j = 0; j < hiddenDim; j++) {
            predQ += w2[action][j] * h1[j];
        }

        double error = predQ - target; // gradient dL/d(predQ)
        double loss = 0.5 * error * error;

        // Clip error to [-1.0, 1.0] for gradient stability (Huber loss effect)
        double clippedError = Math.max(-1.0, Math.min(1.0, error));

        // Gradients for Output layer (action neuron only)
        double[] dW2_action = new double[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            dW2_action[j] = clippedError * h1[j];
        }
        double dB2_action = clippedError;

        // Backpropagate to hidden layer
        double[] dZ1 = new double[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            // ReLU derivative: 1 if z1 > 0 else 0
            double reluGrad = (z1[j] > 0) ? 1.0 : 0.0;
            dZ1[j] = clippedError * w2[action][j] * reluGrad;
        }

        // Apply weight updates: Layer 2
        for (int j = 0; j < hiddenDim; j++) {
            w2[action][j] -= learningRate * dW2_action[j];
        }
        b2[action] -= learningRate * dB2_action;

        // Apply weight updates: Layer 1
        for (int i = 0; i < hiddenDim; i++) {
            for (int j = 0; j < inputDim; j++) {
                w1[i][j] -= learningRate * dZ1[i] * input[j];
            }
            b1[i] -= learningRate * dZ1[i];
        }

        return loss;
    }

    public synchronized void copyWeightsFrom(NeuralNetwork source) {
        for (int i = 0; i < hiddenDim; i++) {
            System.arraycopy(source.w1[i], 0, this.w1[i], 0, inputDim);
            this.b1[i] = source.b1[i];
        }
        for (int i = 0; i < outputDim; i++) {
            System.arraycopy(source.w2[i], 0, this.w2[i], 0, hiddenDim);
            this.b2[i] = source.b2[i];
        }
    }
}
