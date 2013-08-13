package MML;

import java.util.HashMap;

/**
 * Class representing matrices, vectors and scalars, used for the Matrix Micro Language (MML) parsing
 * @author lejlot
 */
public class MathData {
    
    private final static short MISSING_ZERO=0;
    private final static short MISSING_WRAP=1;
    private final static short MISSING_COPY=2;
    
    
    private int rows, cols;
    private float data[][];
    
    public static final MathData ZERO = new MathData(0), ONE = new MathData(1), TWO = new MathData(2);
    
    /**
     * Converts scalar to its float representation
     * @return float value of the scalar
     * @throws Exception if called on the non-scalar type
     */
    public float toFloat() throws Exception{
        if (rows>1||cols>1) throw new Exception("Matrix used in operation requiring scalar value");
        return data[0][0];
    }
     /**
     * Constructs new scalar
     * @param f  value of the scalar
     */
    public MathData(int f){
        //rows=cols=0;
        rows=cols=1;
        data = new float[][]{{f}};
    }
    
    /**
     * Constructs new scalar
     * @param f  value of the scalar
     */
    public MathData(float f){
        //rows=cols=0;
        rows=cols=1;
        data = new float[][]{{f}};
    }
    
    /**
     * Constructs new scalar
     * @param f  value of the scalar
     */
    public MathData(double f){
        //rows=cols=0;
        rows=cols=1;
        data = new float[][]{{(float)f}};
    }
    
    /**
     * Checks if given MathData is a scalar
     * @return true iff this object is a scalar
     */
    public boolean isScalar(){
        //return cols==0;
        return cols==1 && rows==1;
    }
    
    /**
     * Checks if given MathData is a vector (horizontal or vertical)
     * @return true iff this object is a vector
     */
    public boolean isVector(){
        return cols==1 || rows==1;
    }
    
    /**
     * Checks if given MathData is a matrix (in particular each vector is a matrix)
     * @return true iff this object is a matrix
     */
    public boolean isMatrix(){
        return !isScalar();
    }
    
    /**
     * Retrives value from the matrix
     * @param row row number
     * @param col column number
     * @return  float value of the given position or 0 if position is incorrect
     */
    public float get(int row, int col){        
        return get(row,col,MISSING_ZERO);
    }
    
    /**
     * Retrives value from the matrix
     * @param row  row row number
     * @param col column number
     * @param missing values behaviour, MISSING_ZERO - returns 0, MISSING_COPY - returns value from the border, MISSING_WRAP - treats a matrix as a torus
     * @return float value of the given position
     */
    public float get(int row, int col, short missing){
        switch (missing){
            case MISSING_ZERO: if (row<1 || col<1 || row>rows || col>cols) return .0f; break;
            case MISSING_WRAP: while (row<1) row+=rows;
                               while (col<1) col+=cols;
                               while (row>rows) row-=rows;
                               while (col>cols) col-=cols;
                               break;
            case MISSING_COPY: if (row<1) row=1; if (col<1) col=1;
                               if (row>rows) row=rows; if (col>cols) col=cols;
                               break;
        }
        return data[row-1][col-1];
    }
    
    /**
     * Transposes matrix
     * @return Transposed matrix
     */
    public MathData transpose(){
        if (isScalar()) return this;
        float[] dataTransposed = new float[rows*cols];
        for (int j=0; j<cols; ++j)
            for (int i=0; i<rows; ++i)
                dataTransposed[ j * rows + i ] = data[i][j];
        return new MathData(cols, rows, dataTransposed);
    }

    /**
     * Constructs the new MathData object filled with provided values
     * @param rows number of rows
     * @param cols number of columns
     * @param data  1D array containing matrix values (A[i][j] = data[i*cols+j])
     */
    public MathData(int rows, int cols, float[] data){
        this.rows=rows;
        this.cols=cols;
        this.data = new float[rows][cols];
        for (int i=0; i<rows; ++i)
            for (int j=0; j<cols; ++j)
                this.data[i][j] = data[i*cols + j];
    }
    
    /**
     * Constructs the new MathData object filled with provided values
     * @param rows number of rows
     * @param cols number of columns
     * @param data  value to fill entire matrix (A[i][j] = data)
     */
    public MathData(int rows, int cols, float data){
        this.rows=rows;
        this.cols=cols;
        this.data = new float[rows][cols];
        for (int i=0; i<rows; ++i)
            for (int j=0; j<cols; ++j)
                this.data[i][j] = data;
    }
    
    @Override
    public String toString(){
        if (isScalar()) try {
            return toFloat()+"";
        } catch (Exception ex) {
            
        }
        String code = "[";
        for (int i=0; i<rows; ++i){
            for (int j=0;j<cols; ++j){
                code += data[i][j];
                if (j!=cols-1) code+=", ";
            }
            if (i!=rows-1) code += "; ";
        }
        return code +"]";
    }
    
    /**
     * Converts matrix to integer
     * @return integer value
     * @throws Exception thrown if conversion is impossible (e.g. non-scalar value)
     */
    public int toInt() throws Exception{
        return (int)toFloat();
    }
    
    /**
     * Shorter form of matrix multiplication, e.g A.pow(3) == A.mul(A).mul(A)
     * @param exponent exponent value
     * @return value of A^x
     */
    public MathData pow(int exponent) {
        MathData result = new MathData(this);
        try{
            for (int i=1; i<exponent; ++i){
                result = this.mul(result);
            }
        }catch(Exception e){}
        return result;
    }
    
    /**
     * Element-wise powering, e.g. [1,2;3,4] .^ 2 == [1,4;9,16]
     * @param exponent exponent value
     * @return result of the operation
     */
    public MathData ppow(float exponent) {
        MathData result = new MathData(this);
        for (int i=0; i<result.getRows(); ++i) 
            for(int j=0; j<result.getCols(); ++j) 
                result.data[i][j]=(float)Math.pow(result.data[i][j], exponent);
        return result;
    }
    
    /**
     * Performs modulo operation (e.g. 4 mod 2==0, 5 mod 3 == 2, -3 mod 2 == 1)
     * @param m scalar argument do modulo operation
     * @return smallest positive integer from modulo class
     * @throws Exception if m is not scalar or m == 0
     */
    public MathData mod(MathData m)throws Exception{
        if (!m.isScalar()) throw new Exception("Second argument od modulo operation has to be a scalar");
        if (m.toFloat() == .0f) throw new Exception("Modulo zero operation is not permitted");
        MathData result = new MathData(this);
        for (int i=0; i<this.getRows(); ++i)
            for (int j=0; j<this.getCols(); ++j){
                result.data[i][j] = result.data[i][j] % m.toFloat();
                if (result.data[i][j]<0) result.data[i][j] = result.data[i][j] + m.toFloat();
            }
        return result;
    }
    
    /**
     * Performs point-wise modulo operation
     * @param m matrix of the same size 
     * @return matrix of results of modulo operations
     * @throws Exception if any element of m is 0
     */
    public MathData pmod(MathData m) throws Exception{
        if (m.getCols()!=getCols() || m.getRows() != getRows()) throw new Exception("Modulo operation can be applied only to matrices of the same dimensions");
        MathData result = new MathData(this);
        for (int i=0; i<this.getRows(); ++i)
            for (int j=0; j<this.getCols(); ++j){
                result.data[i][j] = result.data[i][j] % m.data[i][j];
                if (result.data[i][j]<0) result.data[i][j] = result.data[i][j] + m.data[i][j];
            }
        return result;
    }
    
    /**
     * Multiplies matrix (or scalar) by matrix (or scalar)
     * @param m matrix (or scalar) to be multiplied by
     * @return result of the operation
     * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
     */
    public MathData mul(MathData m) throws Exception{
        if (isScalar()){
            if (m.isScalar()) 
                return new MathData(m.toFloat()*toFloat());
            else
                return m.mul(this.toFloat());
        }
        if (m.isScalar()) return mul(m.toFloat());
        if (cols != m.getRows()) throw new Exception("Only m x n and n x k matrices can be multiplied");
        float[] values = new float[rows*m.getCols()];
        for (int i=0; i<rows; ++i)
            for (int j=0; j<m.getCols(); ++j)
                for (int k=0; k<cols; ++k)
                    values[i*m.getCols()+j] += data[i][k] * m.data[k][j];
        return new MathData(rows, m.getCols(), values);
    }
    
    /**
     * Sums matrices (or scalars)
     * @param m matrix (or scalars) to add
     * @return result of the operation
     * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
     */
    public MathData add(MathData m) throws Exception{
        if (isScalar()){
            if (m.isScalar()) 
                return new MathData(m.toFloat()+toFloat());
            else
                return m.add(this.toFloat());
        }
        if (m.isScalar()) return add(m.toFloat());
        if (cols != m.getCols() || rows != m.getRows()) throw new Exception("Only m x n and m x n matrices can be added");
        float[] values = new float[rows*m.getCols()];
        for (int i=0; i<rows; ++i)
            for (int j=0; j<m.getCols(); ++j)
                 values[i*m.getCols()+j] += data[i][j] + m.data[i][j];
        return new MathData(rows, m.getCols(), values);
    }
    
    /**
     * Element-wise multiplication of matrices (or scalars), e.g. [1,2] .* [4,5] == [4,10]
     * @param m matrix (or scalar) to be multiplied by
     * @return result of the operation
     * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
     */
    public MathData pmul(MathData m) throws Exception{
        if (isScalar() && m.isScalar()){ 
                return new MathData(m.toFloat()*toFloat());
        }
        
        if (cols != m.getCols() || rows != m.getRows()) throw new Exception("Only m x n and m x n matrices can be added");
        float[] values = new float[rows*m.getCols()];
        for (int i=0; i<rows; ++i)
            for (int j=0; j<m.getCols(); ++j)
                 values[i*m.getCols()+j] += data[i][j] * m.data[i][j];
        return new MathData(rows, m.getCols(), values);
    }
    
    /**
     * Element-wise division of matrices (or scalars), e.g. [1,2] ./ [4,5] == [0.25,0.4]
     * @param m matrix (or scalar) to be divided by
     * @return result of the operation
     * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
     */
    public MathData pdivide(MathData m) throws Exception{
        if (isScalar() && m.isScalar()){ 
                if (m.toFloat()==.0f) throw new Exception("Cannot divide by zero");
                return new MathData(toFloat()/m.toFloat());
        }
        
        if (cols != m.getCols() || rows != m.getRows()) throw new Exception("Only m x n and m x n matrices can be divided");
        float[] values = new float[rows*m.getCols()];
        for (int i=0; i<rows; ++i)
            for (int j=0; j<m.getCols(); ++j){
                if (m.data[i][j]==.0f) throw new Exception("Cannot divide by zero");
                 values[i*m.getCols()+j] = data[i][j] / m.data[i][j];
            }
        return new MathData(rows, m.getCols(), values);
    }
    
    /**
     * Converts object to boolean
     * @return true iff object represents scalar value 0 (or 1x1 matrix with 0)
     */
    public boolean toBoolean(){
        //return rows!=0 || cols !=0 || data[0][0] != .0f;
        return rows!=1 || cols !=1 || data[0][0] != .0f;
    }
    
    /**
     * Sets particular value in vector
     * @param x coordinate
     * @param value  value to set
     */
    public void set(int x, float value){
        if (isScalar()){ data[0][0] = value; return; }
        if (rows==1){
            data[0][x-1]=value;
        }else{
            data[x-1][0]=value;
        }
    }
    
    /**
     * Sets particular value in matrix
     * @param x row coordinate
     * @param y column coordinate
     * @param value  value to set
     */
    public void set(int x, int y, float value){
        if (isScalar()) { data[0][0]=value; return; }
        data[x-1][y-1]=value;
    }
    
    /**
     * Sets one matrix as a submatrix of another matrix
     * @param x row coordinate
     * @param y column coordinate
     * @param m matrix to substitute for
     */
    public void set(int x, int y, MathData m){
        for (int i=x-1; i<x-1+m.rows; ++i){
            for (int j=y-1; j<y-1+m.cols; ++j){
                data[i][j] = m.data[i-x+1][j-y+1];
                //set(i,j,m.get(i-x).get(j-y))
            }
        }
    }
    
    /**
     * Constructs the exact copy of given object
     * @param obj reference object
     */
    public MathData(MathData obj) {
        rows=obj.getRows();
        cols=obj.getCols();
        data=new float[rows][cols];
        for (int i=0; i<rows; ++i)
            for (int j=0; j<cols; ++j)
                data[i][j]=obj.data[i][j];
    }
    
    /**
     * Multiples by scalar
     * @param f scalar value
     * @return result of the operation
     */
    public MathData mul(float f) {
        try {
            if (isScalar()) return new MathData(this.toFloat()*f);
        }catch(Exception e){}
        MathData result = new MathData(this);
        for (int i=0; i<rows; ++i)
            for (int j=0; j<cols; ++j)
                result.data[i][j]=result.data[i][j]*f;
        return result;
    }
    
    /**
     * Adds scalar
     * @param f scalar value
     * @return result of the operation
     */
    public MathData add(float f) {
        MathData result = new MathData(this);
        for (int i=0; i<rows; ++i)
            for (int j=0; j<cols; ++j)
                result.data[i][j]=result.data[i][j]+f;
        return result;
    }
    
    /**
    * Divides by scalar
    * @param m scalar value
    * @return result of the operation
    * @throws  Exception if m is not scalar
    */
    public MathData divide(MathData m) throws Exception{
        if (!m.isScalar()) throw new Exception("Only scalar division is possible");
        if (m.toFloat()==.0f) throw new Exception("Cannot divide by zero");
        return mul(1.0f/m.toFloat());
    }
    
     /**
    * Substraction of matrices (or scalars), 
    * @param m matrix (or scalar) to be substracted
    * @return result of the operation
    * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
    */
    public MathData subtract(MathData m) throws Exception{
        return add(m.negate());
    }
    
     /**
    * Substraction of scalar
    * @param f scalar value
    * @return result of the operation
    * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
    */
    public MathData subtract(float f) throws Exception{
        return add(-f);
    }
    
    /**
     * Retirns -A
     * @return negation 
     */
    public MathData negate() {
        return mul(-1.0f);
    }
    
    /**
     * Sums all elements of the matrix
     * @param m matrix (or scalar) 
     * @return sum of all elements (for scalar value f it is defined as a function returning f)
     */
    public static MathData sum(MathData m){        
        float sum = .0f;
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                sum += m.data[i][j];
        return new MathData(sum);
    }
    
    /**
     * Dimensions of given matrix
     * @param m matrix (or scalar)
     * @return 1 by 2 matrix with dimensions ( size([1,2,3;4,5,6]) == [1,2] , size(5) == [1,1] )
     */
    public static MathData size(MathData m){
        return new MathData(1,2,new float[]{m.getRows(), m.getCols()});
    }
    
    /**
     * Number of elements in the matrix
     * @param m matrix (or scalar)
     * @return number of elements (1 for scalar)
     */
    public static MathData count(MathData m) {
        try {
            return size(m).get(1).mul(size(m).get(2));
        } catch (Exception e){ return new MathData(1); }
    }
    
    /**
     * Mean value of elements in the matrix
     * @param m matrix (or scalar)
     * @return mean value of elements
     */
    public static MathData mean(MathData m) {       
        try {
            return sum(m).divide(count(m)) ;
        }catch(Exception e){ return new MathData(0); }
    }
    
    /**
     * Element-wise square root of matrix
     * @param m matrix (or scalar)
     * @return  matrix (or scalar) with square rooted elements
     */
    public static MathData sqrt(MathData m) {
        MathData result = new MathData(m);
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                result.data[i][j]=(float)Math.sqrt(result.data[i][j]);
        return result;
    }
    
    /**
     * Element-wise maximum function, e.g. max( [1,2;3,4], [5,-2;-1,9] ) == [5,2;3,9]
     * @param m first matrix (or scalar)
     * @param y second matrix(or scalar)
     * @return element-wise maximum
     * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
     */
    public static MathData max(MathData m, MathData y) throws Exception{
        if (m.isScalar() && y.isMatrix()){
            MathData result = new MathData(m);
            for (int i=0; i<result.getRows(); ++i)
                for (int j=0; j<result.getCols(); ++j)
                    result.data[i][j]=Math.max(result.data[i][j],m.toFloat());
            return result;
        }
        if (m.isMatrix() && y.isScalar()) return max(y,m);
        MathData result = new MathData(m);
        
        if (m.getCols() != y.getCols() || m.getRows() != y.getRows()) throw new Exception("Max is not defined for matrices of different sizes");
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                result.data[i][j]=Math.max(m.data[i][j], y.data[i][j]);
        return result;
    }
    
    
    /**
     * Element-wise mean function, e.g. mean( [1,2;3,4], [2,3;4,5] ) == [1.5,2.5;3.5,4.5]
     * @param m first matrix (or scalar)
     * @param y second matrix(or scalar)
     * @return element-wise mean
     * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
     */
    public static MathData mean(MathData m, MathData y) throws Exception{
        MathData result = new MathData(m);
        if (m.getCols() != y.getCols() || m.getRows() != y.getRows()) throw new Exception("Mean is not defined for matrices of different sizes");
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                result.data[i][j]=(m.data[i][j] + y.data[i][j])/2.0f;
        return result;
    }
    
    /**
     * Element-wise minimum function, e.g. min( [1,2;3,4], [5,-2;-1,9] ) == [1,-2;-1,4]
     * @param m first matrix (or scalar)
     * @param y second matrix(or scalar)
     * @return element-wise minimum
     * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
     */
    public static MathData min(MathData m, MathData y) throws Exception{
        if (m.isScalar() && y.isMatrix()){
            MathData result = new MathData(m);
            for (int i=0; i<result.getRows(); ++i)
                for (int j=0; j<result.getCols(); ++j)
                    result.data[i][j]=Math.min(result.data[i][j],m.toFloat());
            return result;
        }
        if (m.isMatrix() && y.isScalar()) return min(y,m);
        MathData result = new MathData(m);
        
        if (m.getCols() != y.getCols() || m.getRows() != y.getRows()) throw new Exception("Max is not defined for matrices of different sizes");
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                result.data[i][j]=Math.min(m.data[i][j], y.data[i][j]);
        return result;
    }
    
    /**
     * Maximum value of given matrix (or scalar)
     * @param m matrix (or scalar)
     * @return maximum element
     */
    public static MathData max(MathData m) {        
        float max=m.data[0][0];
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                if (max<m.data[i][j]) max = m.data[i][j];
        return new MathData(max);
    }
    
    /**
     * Minimum value of given matrix (or scalar)
     * @param m matrix (or scalar)
     * @return minimum element
     */
    public static MathData min(MathData m) {
        float min=m.data[0][0];
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                if (min>m.data[i][j]) min = m.data[i][j];
        return new MathData(min);
    }
    
    /**
     * Equivalent of add(MathData.ONE)
     */
    public static void inc(MathData m){
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                m.data[i][j]=m.data[i][j]+1;
    }
    
    /**
     * Equivalent of substract(MathData.ONE)
     */
    public static void dec(MathData m){
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                m.data[i][j]=m.data[i][j]-1;
    }
    
    /**
     * Converts matrix to vector
     * @return vector of concatenated matrix rows     
     */
    public MathData toVector(){
        if (isScalar()) try {
            return new MathData(1,1,new float[]{toFloat()});
        } catch (Exception ex) {}
        if (isVector()) {
           if (getRows()==1) 
               return this;
           else 
               return transpose();
        }
        float[] values = new float[rows*cols];
        for (int i=0; i<data.length; ++i)
            for (int j=0; j<data[i].length; ++j)
                values[i*cols+j] =data[i][j];
        return new MathData(1,values.length,values);
    }
    
    /**
     * Converts to float[]
     * @return float array of concatenated matrix rows     
     */
    public float[] toFloatArray() {
        return (toVector().data[0]);
    }
    
    /**
     * Concatenates two matrices (or scalars)
     * @param m matrix (or scalar) to concatenate with
     * @param preferSide true iff left-right concatenation
     * @return concatenated matrices (or scalars)
     * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
     */
    public MathData concat(MathData m, boolean preferSide) throws Exception{
        if (isScalar()){
            if (m.isScalar()){
                if (preferSide)
                    return new MathData(1,2,new float[]{toFloat(), m.toFloat()});
                else
                    return new MathData(2,1,new float[]{toFloat(), m.toFloat()});
            }
            if (m.isVector()){
                if (preferSide && m.getRows()==1){
                    float[] values = new float[m.getCols()+1];
                    values[0] = toFloat();
                    for (int i=0; i<m.getCols(); ++i) values[i+1]=m.get(i+1).toFloat(); 
                    return new MathData(1,values.length, values);
                }
                if (!preferSide && m.getCols() == 1){
                    float[] values = new float[m.getRows()+1];
                    values[0] = toFloat();
                    for (int i=0; i<m.getRows(); ++i) values[i+1]=m.get(i+1).toFloat(); 
                    return new MathData(values.length,1, values);                    
                }
            }
            throw new Exception("Scalar cannot be concatenated to a matrix");
        }
        
        if (isVector()){
            if (m.isScalar()){
                if (preferSide && getRows()==1){
                    float[] values = new float[getCols()+1];
                    values[values.length-1] = m.toFloat();
                    for (int i=0; i<getCols(); ++i) values[i]=get(i+1).toFloat(); 
                    return new MathData(1,values.length, values);
                }
                if (!preferSide && getCols()==1){
                    float[] values = new float[getRows()+1];
                    values[values.length-1] = m.toFloat();
                    for (int i=0; i<getRows(); ++i) values[i]=get(i+1).toFloat(); 
                    return new MathData(values.length,1, values);                    
                }
            }            
        }
        
        if (getRows() ==m.getRows() && (preferSide)){ // concat left-right
            float[] values = new float[ getRows()*getCols() + m.getRows()*m.getCols() ];
            for (int i=0; i<getRows(); ++i)
                for (int j=0; j<getCols(); ++j){
                    values[ i * (getCols() + m.getCols()) + j ] = data[i][j];                    
                }
            for (int i=0; i<m.getRows(); ++i)
                for (int j=0; j<m.getCols(); ++j){
                    values[ i * (getCols() + m.getCols()) + j + getCols() ] = m.data[i][j];                    
                }
            return new MathData(getRows(), getCols()+m.getCols(), values);
        }
        
        if (getCols() == m.getCols() && !preferSide){ // top-down
            float[] values = new float[ getRows()*getCols() + m.getRows()*m.getCols() ];
            for (int i=0; i<getRows(); ++i)
                for (int j=0; j<getCols(); ++j){
                    values[ i * getCols() + j ] = data[i][j];                    
                }
            for (int i=0; i<m.getRows(); ++i)
                for (int j=0; j<m.getCols(); ++j){
                    values[ (i+getRows()) * getCols() + j ] = m.data[i][j];                    
                }
            return new MathData(getRows() + m.getRows(), getCols(), values);            
        }
        
        throw new Exception("This two objects cannot be concatenated");
    }
    
    /**
     * Prints matrix with, each row in a separate line
     */
    public void print(){
        for (int i=0; i<getRows(); ++i){
                for (int j=0; j<getCols(); ++j){
                    System.out.print(data[i][j] + " ");
                }
                System.out.println();
        }
    }
    
    /**
     * Returns x'th element - for matrices: x'th row, for vectors: x'th element
     * @param x coordinate
     * @return x'th row as a vector (for matrices) or x'th element as scalar (for vectors)
     */
    public MathData get(int x) {
        if (isScalar()) return this;
        if (isVector()){
            if (rows==1){
                return new MathData(data[0][x-1]);
            }else{
                return new MathData(data[x-1][0]);
            }
        }
        return new MathData(1,cols,data[x-1]);
    }

    /**
     * Returns number of rows
     * @return number of rows
     */
    public int getRows(){ return rows; }
    
    /**
     * Returns number of columns
     * @return number of columns
     */
    public int getCols(){ return cols; }
    
    /**
     * Performs substitution of form A[x] = value, this is corrent for A-matrix, value-vector or A-vector, value-scalar
     * @param x coordinate
     * @param value value to substitute for
     * @throws Exception thrown when operation is impossible (e.g. due to incorrect matrices sizes)
     */
    public void set(int x, MathData value) throws Exception{
        if (isScalar()) data[0][0] = value.toFloat();
        if (isVector()){
            if (rows==1){
                data[0][x-1] = value.toFloat();
            }else{
                data[x-1][0] = value.toFloat();
            }
            return;
        }
        if (!value.isVector()) throw new Exception("Cannot matrix or scalar as a row or column of another matrix");
        if (value.getRows()==1){
            for (int i=0; i<cols; ++i){
                data[x-1][i] = value.get(i+1).toFloat();
            }
        }else{
            for (int i=0; i<rows; ++i){
                data[i][x-1] = value.get(i+1).toFloat();
            }
        }
    }

    /**
     * Logical AND operation on MathData objects (MathData.ZERO == false, MathData.ONE == true)
     * @return MathData.ONE if its true and MathData.ZERO otherwise
     */
    static public MathData and(MathData m, MathData y){
        return (m.toBoolean() && y.toBoolean()) ? MathData.ONE : MathData.ZERO;
    }
    
    /**
     * Logical OR operation on MathData objects (MathData.ZERO == false, MathData.ONE == true)
     * @return MathData.ONE if its true and MathData.ZERO otherwise
     */
    static public MathData or(MathData m, MathData y){
        return (m.toBoolean() || y.toBoolean()) ? MathData.ONE : MathData.ZERO;
    }
    
    /**
    * Logical NOT operation on MathData objects (MathData.ZERO == false, MathData.ONE == true)
     * @return MathData.ONE if its true and MathData.ZERO otherwise
    */
    static public MathData not(MathData m){
        return (m.toBoolean() ) ? MathData.ZERO : MathData.ONE;
    }
    
    /**
    * Logical Equal operation on MathData objects (MathData.ZERO == false, MathData.ONE == true)
     * @return MathData.ONE if its true and MathData.ZERO otherwise
    */
    static public MathData eq(MathData m, MathData y) {
        if (m.getCols() != y.getCols() || m.getRows() != y.getRows()) return MathData.ZERO;
        if (m.isScalar() && y.isScalar()) try {
            return m.toFloat() == y.toFloat() ? MathData.ONE : MathData.ZERO;
        } catch (Exception ex) {}
        for (int i=0; i<m.getRows(); ++i) for (int j=0; j<m.getCols(); ++j) if (m.data[i][j]!=y.data[i][j]) return MathData.ZERO;
        return MathData.ONE;
    }
    
    /**
    * Logical Less operation on MathData objects (MathData.ZERO == false, MathData.ONE == true)
     * @return MathData.ONE if its true and MathData.ZERO otherwise
    */
    static public MathData le(MathData m, MathData y) {        
        try {
            if (m.isScalar() && y.isMatrix()) return m.toFloat() < max(y).toFloat() ? MathData.ONE : MathData.ZERO;
            if (m.isMatrix() && y.isScalar()) return y.toFloat() < max(m).toFloat() ? MathData.ONE : MathData.ZERO;        
            if (m.getCols() != y.getCols() || m.getRows() != y.getRows()) return MathData.ZERO;
            if (m.isScalar() && y.isScalar()) return ((m.toFloat() < y.toFloat()) ? MathData.ONE : MathData.ZERO);
            for (int i=0; i<m.getRows(); ++i)for (int j=0; j<m.getCols(); ++j) if (m.data[i][j]>=y.data[i][j]) return MathData.ZERO;
        }catch(Exception e){}
        return MathData.ONE;
    }
    
    /**
    * Logical  Less or Equal operation on MathData objects (MathData.ZERO == false, MathData.ONE == true)
     * @return MathData.ONE if its true and MathData.ZERO otherwise
    */    
    static public MathData leq(MathData m, MathData y) {
        return or(le(m,y),eq(m,y));
    }
    
    /**
    * Logical Greater operation on MathData objects (MathData.ZERO == false, MathData.ONE == true)
     * @return MathData.ONE if its true and MathData.ZERO otherwise
    */    
    static public MathData ge(MathData m, MathData y) {
        try{
            if (m.isScalar() && y.isMatrix()) return m.toFloat() > max(y).toFloat() ? MathData.ONE : MathData.ZERO;
            if (m.isMatrix() && y.isScalar()) return y.toFloat() > max(m).toFloat() ? MathData.ONE : MathData.ZERO;        
            if (m.getCols() != y.getCols() || m.getRows() != y.getRows()) return MathData.ZERO;
            if (m.isScalar() && y.isScalar()) return m.toFloat() > y.toFloat() ? MathData.ONE : MathData.ZERO;
            for (int i=0; i<m.getRows(); ++i)for (int j=0; j<m.getCols(); ++j) if (m.data[i][j]<=y.data[i][j]) return MathData.ZERO;
        }catch(Exception e){}
        return MathData.ONE;
    }
    
    /**
    * Logical Greater or Equal operation on MathData objects (MathData.ZERO == false, MathData.ONE == true)
     * @return MathData.ONE if its true and MathData.ZERO otherwise
    */
    static public MathData geq(MathData m, MathData y) {
        return or(ge(m,y),eq(m,y));
    }
    
    /**
     * Calculates product of matrix (or scalar) elements, e.g. prod([1,2,3]) = 6
     * @param m matrix (or scalar)
     * @return product of elements
     */
    static public MathData prod(MathData m){
        float prod = 1.0f;
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                prod *= m.data[i][j];
        return new MathData(prod);    
    }
    
    /**
     * Creates new matrix of zeros of given size
     * @param m size of matrix (scalar or 2-elements big vector)
     * @return matrix filled with zeros
     * @throws Exception thrown if m is not a scalar nor 2-elements big vector
     */
    static public MathData zeros(MathData m) throws Exception{
        if (m.isScalar()) return new MathData(1,m.toInt(),new float[m.toInt()]);
        if (m.isVector()) {
            if (max(size(m)).toInt() == 2)
                return new MathData(m.get(1).toInt(), m.get(2).toInt(),new float[prod(m).toInt()]);
        }
        throw new Exception("Only scalar or 2-dimensional vector can be used as an argument for the zero() function");
    }
    
    /**
     * Equivalent of zeros( new MathData(1,2, new float[]{ m.toFloat(), y.toFloat() }) )
     */
    static public MathData zeros(MathData m, MathData y) throws Exception{
        if (m.isScalar() && y.isScalar()) return new MathData(m.toInt(),y.toInt(),.0f);        
        throw new Exception("Only two scalars can be used as an arguments for the zeros(,) function");
    }

    /**
     * Creates new matrix of ones of given size
     * @param m size of matrix (scalar or 2-elements big vector)
     * @return matrix filled with ones
     * @throws Exception thrown if m is not a scalar nor 2-elements big vector
     */
    static public MathData ones(MathData m) throws Exception{
        if (m.isScalar()) return new MathData(1,m.toInt(),1.0f);
        if (m.isVector()) {
            if (max(size(m)).toInt() == 2)
                return new MathData(m.get(1).toInt(), m.get(2).toInt(),1.0f);
        }
        throw new Exception("Only scalar or 2-dimensional vector can be used as an argument for the ones() function");
    }
    
    /**
     * Equivalent of ones( new MathData(1,2, new float[]{ m.toFloat(), y.toFloat() }) )
     */
    static public MathData ones(MathData m, MathData y) throws Exception{
        if (m.isScalar() && y.isScalar()) return new MathData(m.toInt(),y.toInt(),1.0f);        
        throw new Exception("Only two scalars can be used as an arguments for the ones(,) function");
    }

    /**
     * Creates identity matrix of given size
     * @param m size (as scalar)
     * @return identity matrix of size m
     * @throws Exception thrown if m is not scalar
     */
    static public MathData ident(MathData m) throws Exception{
        if (m.isScalar()){
            MathData I = zeros(m.get(1), m.get(1));
            for (int i=0; i<m.toInt(); ++i) I.data[i][i]=1;
            return I;
        }        
        throw new Exception("Only a scalar can be used as an argument for the ident() function");
    }
    
    /**
     * Gets submatrix in given coordinates and of given size
     * @param m matrix to get submatrix from
     * @param i coordinates of submatrix upper-left corner
     * @param el size of submatrix
     * @return submatrix of m of size el in position i
     * @throws Exception thrown if operattion is impossible (due to for example dimensionality problems)
     */
    static public MathData sub(MathData m, MathData i, MathData el) throws Exception{
        if (i.isVector()){
            if (!eq(count(i),(MathData.TWO)).toBoolean() || !eq(count(el),(MathData.TWO)).toBoolean()) throw new Exception("Only 1x2 or 2x1 vectors can be used as an argument for sub( , , )");
            return sub(m, i.get(1), i.get(2), el.get(1), el.get(2));
        }else{
            if (!i.isScalar() || !el.isScalar()) throw new Exception("Both or none of the arguments have to be scalar");
            return sub(m, i.get(1), MathData.ZERO, el.get(1), new MathData(m.getCols()));
        }
    }

    /**
     *  Equivalent of sub(m, new MathData(1,2,new float[]{i.toFloat(), j.toFloat()}), new MathData(1,2,new float[]{eli.toFloat(), elj.toFloat()}))
     */
    static public MathData sub(MathData m, MathData i, MathData j, MathData eli, MathData elj) throws Exception{
        if (!i.isScalar() || !j.isScalar() || !eli.isScalar() || !elj.isScalar()) throw new Exception("Only scalars can be used with sub ( , , , , , )");
        float[] data = new float[eli.toInt() * elj.toInt()];
        for (int xi=0; xi<eli.toInt(); ++xi)
            for (int xj=0; xj<elj.toInt(); ++xj)
                data[ xi * elj.toInt() + xj ] = m.data[i.toInt()+xi-1][ j.toInt()+xj-1];
        return new MathData(eli.toInt(), elj.toInt(), data);
    }
    
    /**
     * Equivalent of m.toVector()
     */
    static public MathData vectorize(MathData m){
        return m.toVector();
    }
    
    /**
     * Returns absolute value of given matrix
     * @param m input matrix
     * @return matrix of the same dimensions as m, with Math.abs(.) used on each element
     */
    static public MathData abs(MathData m){
        MathData res = new MathData(m);
        for (int i=0; i<m.data.length; ++i)
            for (int j=0; j<m.data[i].length; ++j)
                res.data[i][j] = Math.abs(res.data[i][j]);
        return res;    
    }
    
    /**
     * Performs convolution A*B
     * @param A Matrix to be convolved
     * @param B Kernel
     * @return Matrix being effect of convolution of A and B
     */
    static public MathData conv2(MathData A, MathData B){
        MathData c = new MathData(A.cols+B.cols-1,A.rows+B.rows-1,0);
        for (int y=0; y<c.cols; ++y){
            for (int x=0; x<c.rows; ++x){
                float sum = .0f;
                for (int bx=B.rows-1-Math.max(B.rows-1-x, 0),
                     ax=Math.max(0, x-(B.rows-1));
                     bx >= 0 && ax < A.rows;
                     bx--, ax++
                    ){
                    
                    for (
                            int by = B.cols - 1 - Math.max(0,B.cols-1-y),
                             ay = Math.max(0,y-(B.cols-1));
                             by >= 0 && ay < A.cols; 
                             by--, ay++
                        ){
                        sum+=A.get(ax+1,ay+1,MISSING_ZERO)*B.get(bx+1,by+1,MISSING_ZERO);
                    }
                }
                c.set(x+1, y+1, sum);
            }
        }
        return c;
    }
    
    /**
     * Performs 2d convolution of image and a filter
     * @param A Image to be convolved
     * @param B Filter
     * @return Filtered image, missing values are copied from the border
     */
    static public MathData imconv(MathData A, MathData B){
        return imconv(A,B,MISSING_COPY);
    }
    
    /**
     * Performs 2d convolution of image and a filter
     * @param A Image to be convolved
     * @param B Filter
     * @param type of behavior when there are missing values, MISSING_ZERO - puts 0, MISSING_COPY - copies border value, MISSING_WRAP - treats image as a torus
     * @return Filtered image
     */
    static public MathData imconv(MathData A, MathData B,short type){
        MathData C = new MathData(A);
        int centerX = B.rows/2;
        int centerY = B.cols/2;
        for (int y=1; y<=A.cols; ++y){
            for (int x=1; x<=A.rows; ++x){
                float sum=0;
                for (int ny=1; ny<=B.cols; ++ny){
                    for (int nx=1; nx<=B.rows; ++nx){
                        sum+=A.get(x+nx-1-centerX,y+ny-1-centerY,type)*B.get(nx, ny);
                    }
                }
                C.set(x, y, sum);
            }
        }
        return C;
    }
    
    static private HashMap<String, Integer> lines;
    
    /**
     * Returns current line number, used for debug
     * @param filename name of the file associated with the code
     * @return last set line number
     */
    static public int getLineNumber(String filename){
        if (lines==null) lines = new HashMap<String, Integer>();
        return lines.get(filename);
    }
    
    /**
     * Sets current line number, used for debug
     * @param line line number
     * @param filename  name of the file associated with the code
     */
    static public void setLineNumber(int line, String filename){
        if (lines==null) lines = new HashMap<String, Integer>();
        if (filename==null) filename="Current file";
        lines.put(filename, line);
    }
    
    /**
     * Calculates sinus of all values in the given matrix
     * @param A matrix of sinus arguments
     * @return matrix of corresponding sinus values
     */
    static public MathData sin(MathData A){
        MathData X = new MathData(A);
        for (int y=1; y<=A.cols; ++y){
            for (int x=1; x<=A.rows; ++x){
                X.set(x,y, (float) Math.sin(A.get(x,y)));
            }
        }        
        return X;
    }
    
    
    
    /**
     * Calculates cosinus of all values in the given matrix
     * @param A matrix of cosinus arguments
     * @return matrix of corresponding cosinus values
     */
    static public MathData cos(MathData A){
        MathData X = new MathData(A);
        for (int y=1; y<=A.cols; ++y){
            for (int x=1; x<=A.rows; ++x){
                X.set(x,y, (float) Math.cos(A.get(x,y)));
            }
        }
        return X;
    }
    
    /**
     * Calculates tangent of all values in the given matrix
     * @param A matrix of tangent arguments
     * @return matrix of corresponding tangent values
     */
    static public MathData tg(MathData A){
        MathData X = new MathData(A);
        for (int y=1; y<=A.cols; ++y){
            for (int x=1; x<=A.rows; ++x){
                X.set(x,y, (float) Math.tan(A.get(x,y)));
            }
        }
        return X;
    }
    
    /**
     * Calculates cotangent of all values in the given matrix
     * @param A matrix of cotangent arguments
     * @return matrix of corresponding cotangent values
     */    
    static public MathData ctg(MathData A){
        MathData X = new MathData(A);
        for (int y=1; y<=A.cols; ++y){
            for (int x=1; x<=A.rows; ++x){
                X.set(x,y, 1/(float) Math.tan(A.get(x,y)));
            }
        }
        return X;
    }
    
    /**
     * Calculates ceil of all values in the given matrix
     * @param A matrix of ceil arguments
     * @return matrix of corresponding ceil values
     */    
    static public MathData ceil(MathData A){
        MathData X = new MathData(A);
        for (int y=1; y<=A.cols; ++y){
            for (int x=1; x<=A.rows; ++x){
                X.set(x,y, (float) Math.ceil(A.get(x,y)));
            }
        }
        return X;
    }
    
    /**
     * Calculates exponent of all values in the given matrix
     * @param A matrix of exponent arguments
     * @return matrix of corresponding exponent values
     */    
    static public MathData exp(MathData A){
        MathData X = new MathData(A);
        for (int y=1; y<=A.cols; ++y){
            for (int x=1; x<=A.rows; ++x){
                X.set(x,y, (float) Math.exp(A.get(x,y)));
            }
        }
        return X;
    }
 
    /**
     * Creates a sequence of values from current one to given parameter
     * @param A upper limit of the sequence
     * @return vector of values from this to A
     * @throws Exception if this or A are not scalars
     */
    public MathData to(MathData A) throws Exception{
        if (!this.isScalar() || !A.isScalar()) 
            throw new Exception("Operator : can only be applied to scalars");
        int size=(int)(Math.abs(A.toFloat()-this.toFloat())+1);
        float[] seq = new float[size];
        int id=0;
        if (this.toFloat() < A.toFloat())
            for (float now=this.toFloat(); now<=A.toFloat(); ++now) 
                seq[id++]=now;
        else
            for (float now=this.toFloat(); now>=A.toFloat(); --now) 
                seq[id++]=now;
        return new MathData(1,size,seq);
    }
    
    /**
     * Performs times side concatenations with itself
     * @param times of concatenations
     * @return matrix being a result of times side self concatenations
     */
    public MathData sideconcat(int times){        
        MathData expanded = new MathData(rows,cols*times,0f);
        for (int row=1; row<=rows; ++row){
            for (int col=1; col<=cols; ++col){
                for (int i=0; i<times; ++i){
                    expanded.set(row, cols*i+col, get(row,col));
                }
            }
        }
        return expanded;
    }
    
    /**
     * Performs times bottom concatenations with itself
     * @param times of concatenations
     * @return matrix being a result of times bottom self concatenations
     */
    public MathData bottomconcat(int times){        
        MathData expanded = new MathData(rows*times,cols,0f);
        for (int row=1; row<=rows; ++row){
            for (int col=1; col<=cols; ++col){
                for (int i=0; i<times; ++i){
                    expanded.set(rows*i+row, col, get(row,col));
                }
            }
        }
        return expanded;
    }
    
}

