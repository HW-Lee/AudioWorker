#include <iostream>
#include <vector>
#include <cmath>

#ifndef __MATRIX_HEADER__
#define __MATRIX_HEADER__

#define MIN_EPS 1e-13

typedef struct {
    unsigned int nrows;
    unsigned int ncols;
} mat_dim_t;

typedef enum {
    MAT_ROW,
    MAT_COL
} mat_id_t;

template<class T> class Matrix {
public:
    ~Matrix();
    Matrix();
    Matrix(unsigned int nrows, unsigned int ncols);

    Matrix<T> operator=(const T *values);
    Matrix<T> operator=(const Matrix<T>& m);

    Matrix<T> operator+(const Matrix<T>& m);
    Matrix<T> operator-(const Matrix<T>& m);
    Matrix<T> operator*(const Matrix<T>& m);

    Matrix<T> operator+(const T v);
    Matrix<T> operator-(const T v);
    Matrix<T> operator*(const T v);
    Matrix<T> operator/(const T v);

    Matrix<T> operator+=(const Matrix<T>& m);
    Matrix<T> operator-=(const Matrix<T>& m);

    Matrix<T> operator+=(const T v);
    Matrix<T> operator-=(const T v);
    Matrix<T> operator*=(const T v);
    Matrix<T> operator/=(const T v);

    template<class U>
    Matrix<U> convert();

    Matrix<T> sub_mat(unsigned int ridx_from, unsigned int ridx_to, unsigned int cidx_from, unsigned int cidx_to);

    T* operator[](const int idx);

    Matrix<double> pinv();

    Matrix<T> diag();
    Matrix<T> transpose();
    mat_dim_t getDimension();

    void element_op(mat_id_t row_or_col, int idx_from, int idx_to, T weight);
    void gram_schmidt_process(Matrix<double>& A, Matrix<double>& B);
    void LU_decomp(Matrix<double>& L, Matrix<double>& U);
    void QR_decomp(Matrix<double>& Q, Matrix<double>& R);
    void svd(Matrix<double>& U, Matrix<double>& S, Matrix<double>& V);

    void gaussian_row_elimination(Matrix<double>& A, Matrix<double>& B, bool inv);

    double norm();

    static T epsilon(Matrix<T> m);
    static Matrix<T> identity(unsigned size);
    static Matrix<double> eigenvalues(Matrix<T> mat);
    static Matrix<double> eigenvectors(Matrix<T> mat, Matrix<double>& eig_values);

private:
    T* raw;
    mat_dim_t dim;
};

template<class T>
Matrix<T>::Matrix() {
    this->dim.nrows = 0;
    this->dim.ncols = 0;
    this->raw = NULL;
}

template<class T>
Matrix<T>::Matrix(unsigned int nrows, unsigned int ncols) {
    this->dim.nrows = nrows;
    this->dim.ncols = ncols;
    this->raw = (T*) new char[nrows * ncols * sizeof(T)];
    for (int i = 0; i < nrows * ncols; i++)
        this->raw[i] = 0;
}

template<class T>
Matrix<T>::~Matrix() {
}

template<class T>
T Matrix<T>::epsilon(Matrix<T> m) {
    T v = 0;
    for (int i = 0; i < m.getDimension().nrows; i++) {
        for (int j = 0; j < m.getDimension().ncols; j++) {
            if (std::abs(m[i][j]) > v)
                v = std::abs(m[i][j]);
        }
    }

    return (T) (v * MIN_EPS);
}

template<class T>
double Matrix<T>::norm() {
    double normsq = 0;
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        normsq += this->raw[i] * this->raw[i];

    return std::sqrt(normsq);
}

template<class T>
Matrix<T> Matrix<T>::operator=(const T *values) {
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        this->raw[i] = values[i];

    return *this;
}

template<class T>
Matrix<T> Matrix<T>::operator=(const Matrix<T>& m) {
    if (this->raw != NULL)
        delete[] this->raw;

    this->dim.nrows = m.dim.nrows;
    this->dim.ncols = m.dim.ncols;
    this->raw = (T*) new char[m.dim.nrows * m.dim.ncols * sizeof(T)];
    for (int i = 0; i < m.dim.nrows * m.dim.ncols; i++)
        this->raw[i] = m.raw[i];

    return *this;
}

template<class T>
Matrix<T> Matrix<T>::operator+(const Matrix<T>& m) {
    if (this->dim.nrows != m.dim.nrows ||
            this->dim.ncols != m.dim.ncols) {
        // blablabla
        return Matrix(0, 0);
    }

    Matrix<T> mat(this->dim.nrows, this->dim.ncols);
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        mat.raw[i] = this->raw[i] + m.raw[i];

    return mat;
}

template<class T>
Matrix<T> Matrix<T>::operator-(const Matrix<T>& m) {
    if (this->dim.nrows != m.dim.nrows ||
            this->dim.ncols != m.dim.ncols) {
        // blablabla
        return Matrix(0, 0);
    }

    Matrix<T> mat(this->dim.nrows, this->dim.ncols);
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        mat.raw[i] = this->raw[i] - m.raw[i];

    return mat;
}

template<class T>
Matrix<T> Matrix<T>::operator*(const Matrix<T>& m) {
    if (this->dim.ncols != m.dim.nrows) {
        // blablabla
        return Matrix(0, 0);
    }

    Matrix<T> mat(this->dim.nrows, m.dim.ncols);

    for (int i = 0; i < this->dim.nrows; i++) {
        for (int j = 0; j < m.dim.ncols; j++) {
            mat[i][j] = 0;
            for (int k = 0; k < this->dim.ncols; k++)
                mat[i][j] += this->raw[i * this->dim.ncols + k] * m.raw[k * m.dim.ncols + j];
        }
    }

    return mat;
}

template<class T>
Matrix<T> Matrix<T>::operator+(const T v) {
    Matrix<T> mat(this->dim.nrows, this->dim.ncols);
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        mat.raw[i] = this->raw[i] + v;

    return mat;
}

template<class T>
Matrix<T> Matrix<T>::operator-(const T v) {
    Matrix<T> mat(this->dim.nrows, this->dim.ncols);
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        mat.raw[i] = this->raw[i] - v;

    return mat;
}

template<class T>
Matrix<T> Matrix<T>::operator*(const T v) {
    Matrix<T> mat(this->dim.nrows, this->dim.ncols);
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        mat.raw[i] = this->raw[i] * v;

    return mat;
}

template<class T>
Matrix<T> Matrix<T>::operator/(const T v) {
    Matrix<T> mat(this->dim.nrows, this->dim.ncols);
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        mat.raw[i] = this->raw[i] / v;

    return mat;
}

template<class T>
Matrix<T> Matrix<T>::operator+=(const Matrix<T>& m) {
    if (this->dim.nrows != m.dim.nrows ||
            this->dim.ncols != m.dim.ncols) {
        // blablabla
        return Matrix(0, 0);
    }

    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        this->raw[i] += m.raw[i];

    return *this;
}

template<class T>
Matrix<T> Matrix<T>::operator-=(const Matrix<T>& m) {
    if (this->dim.nrows != m.dim.nrows ||
            this->dim.ncols != m.dim.ncols) {
        // blablabla
        return Matrix(0, 0);
    }

    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        this->raw[i] -= m.raw[i];

    return *this;
}

template<class T>
Matrix<T> Matrix<T>::operator+=(const T v) {
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        this->raw[i] += v;

    return *this;
}

template<class T>
Matrix<T> Matrix<T>::operator-=(const T v) {
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        this->raw[i] -= v;

    return *this;
}

template<class T>
Matrix<T> Matrix<T>::operator*=(const T v) {
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        this->raw[i] *= v;

    return *this;
}

template<class T>
Matrix<T> Matrix<T>::operator/=(const T v) {
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        this->raw[i] /= v;

    return *this;
}

template<class T>
T* Matrix<T>::operator[](const int idx) {
    return &(this->raw[idx * this->dim.ncols]);
}

template<class T>
template<class U>
Matrix<U> Matrix<T>::convert() {
    Matrix<U> mat(this->dim.nrows, this->dim.ncols);
    for (int i = 0; i < this->dim.nrows * this->dim.ncols; i++)
        mat[i / this->dim.ncols][i % this->dim.ncols] = (U) this->raw[i];

    return mat;
}

template<class T>
mat_dim_t Matrix<T>::getDimension() {
    return this->dim;
}

template<class T>
Matrix<T> Matrix<T>::diag() {
    if (this->dim.ncols != this->dim.nrows)
        return Matrix<T>(0, 0);

    Matrix<T> mat(this->dim.ncols, 1);
    for (int i = 0; i < this->dim.ncols; i++)
        mat[i][0] = this->raw[i * this->dim.ncols + i];

    return mat;
}

template<class T>
Matrix<double> Matrix<T>::pinv() {
    Matrix<double> U, S, V;
    this->svd(U, S, V);
    for (int i = 0; i < S.getDimension().ncols; i++) {
        S[i][i] = 1. / S[i][i];
    }

    return V * S * U.transpose();
}

template<class T>
Matrix<T> Matrix<T>::transpose() {
    Matrix<T> mat(this->dim.ncols, this->dim.nrows);
    for (int i = 0; i < this->dim.nrows; i++)
        for (int j = 0; j < this->dim.ncols; j++)
            mat[j][i] = this->raw[i * this->dim.ncols + j];

    return mat;
}

template<class T>
Matrix<T> Matrix<T>::sub_mat(unsigned int ridx_from, unsigned int ridx_to, unsigned int cidx_from, unsigned int cidx_to) {
    Matrix<T> mat(ridx_to - ridx_from, cidx_to - cidx_from);

    for (int i = 0; i < mat.getDimension().nrows; i++) {
        for (int j = 0; j < mat.getDimension().ncols; j++)
            mat[i][j] = this->raw[(i + ridx_from) * this->dim.ncols + (j + cidx_from)];
    }

    return mat;
}

template<class T>
void Matrix<T>::element_op(mat_id_t row_or_col, int idx_from, int idx_to, T weight) {
    switch (row_or_col) {
        case MAT_ROW:
            if (idx_from != idx_to) {
                for (int i = 0; i < this->dim.ncols; i++)
                    this->raw[idx_to * this->dim.ncols + i] += this->raw[idx_from * this->dim.ncols + i] * weight;
            } else {
                for (int i = 0; i < this->dim.ncols; i++)
                    this->raw[idx_to * this->dim.ncols + i] *= weight;
            }
            break;

        case MAT_COL:
            if (idx_from != idx_to) {
                for (int i = 0; i < this->dim.nrows; i++)
                    this->raw[i * this->dim.ncols + idx_to] += this->raw[i * this->dim.ncols + idx_from] * weight;
            } else {
                for (int i = 0; i < this->dim.nrows; i++)
                    this->raw[i * this->dim.ncols + idx_to] *= weight;
            }
            break;

        default:
            break;
    }
}

template<class T>
void Matrix<T>::gram_schmidt_process(Matrix<double>& A, Matrix<double>& B) {
    A = Matrix<double>(this->dim.nrows, this->dim.nrows);
    B = Matrix<double>(this->dim.nrows, this->dim.ncols);

    for (int i = 0; i < this->dim.nrows; i++)
        A[i][i] = 1;

    for (int i = 0; i < this->dim.nrows; i++) {
        for (int j = 0; j < this->dim.ncols; j++) {
            B[i][j] = this->raw[i * this->dim.ncols + j];
        }
    }

    for (int i = 1; i < this->dim.nrows; i++) {
        for (int row_idx = 0; row_idx < i; row_idx++) {
            double dot = 0;
            double normsq = 0;
            for (int j = 0; j < this->dim.ncols; j++) {
                dot += B[i][j] * B[row_idx][j];
                normsq += B[row_idx][j] * B[row_idx][j];
            }

            if (std::sqrt(normsq) < Matrix<double>::epsilon(B))
                continue;

            double weight = -dot / normsq;
            B.element_op(MAT_ROW, row_idx, i, weight);
            A.element_op(MAT_COL, i, row_idx, -weight);
        }
    }

    for (int i = 0; i < this->dim.nrows; i++) {
        double norm = 0;
        for (int j = 0; j < this->dim.ncols; j++) {
            norm += (B[i][j] * B[i][j]);
        }

        norm = std::sqrt(norm);
        if (norm < Matrix<double>::epsilon(B)) {
            A.element_op(MAT_COL, i, i, 0);
        } else {
            B.element_op(MAT_ROW, i, i, 1. / norm);
            A.element_op(MAT_COL, i, i, norm);
        }
    }
}

template<class T>
void Matrix<T>::gaussian_row_elimination(Matrix<double>& A, Matrix<double>& B, bool inv) {
    A = Matrix<double>(this->dim.nrows, this->dim.nrows);
    B = Matrix<double>(this->dim.nrows, this->dim.ncols);

    for (int i = 0; i < this->dim.nrows; i++)
        A[i][i] = 1;

    for (int i = 0; i < this->dim.nrows; i++) {
        for (int j = 0; j < this->dim.ncols; j++) {
            B[i][j] = this->raw[i * this->dim.ncols + j];
        }
    }

    int i = 0;
    int offset = 0;
    while (i < this->dim.ncols) {
        int k = i + offset;
        if (k == this->dim.ncols)
            break;

        if (std::abs(B[i][k]) < Matrix<double>::epsilon(B)) {
            for (int j = i+1; j < this->dim.nrows; j++) {
                if (std::abs(B[j][k]) > Matrix<double>::epsilon(B)) {
                    B.element_op(MAT_ROW, j, i, 1);
                    if (inv)
                        A.element_op(MAT_ROW, j, i, 1);
                    else
                        A.element_op(MAT_COL, i, j, -1);
                    break;
                }
            }
            if (std::abs(B[i][k]) < Matrix<double>::epsilon(B)) {
                offset++;
                continue;
            }
        }

        for (int j = i+1; j < this->dim.nrows; j++) {
            double weight = -B[j][k] / B[i][k];
            B.element_op(MAT_ROW, i, j, weight);
            if (inv)
                A.element_op(MAT_ROW, i, j, weight);
            else
                A.element_op(MAT_COL, j, i, -weight);
        }
        i++;
    }
}

template<class T>
void Matrix<T>::LU_decomp(Matrix<double>& L, Matrix<double>& U) {
    this->gaussian_row_elimination(L, U, false);
}

template<class T>
void Matrix<T>::QR_decomp(Matrix<double>& Q, Matrix<double>& R) {
    Matrix<double> m;

    this->LU_decomp(Q, R);
    Q.transpose().gram_schmidt_process(m, Q);
    Q = Q.transpose();
    R = m.transpose() * R;
}

template<class T>
Matrix<T> Matrix<T>::identity(unsigned int size) {
    Matrix<T> mat(size, size);
    for (int i = 0; i < size; i++)
        mat[i][i] = 1;

    return mat;
}

template<class T>
Matrix<double> Matrix<T>::eigenvalues(Matrix<T> mat) {
    if (mat.getDimension().ncols != mat.getDimension().nrows) {
        // blablabla
        return Matrix<double>(0, 0);
    }

    Matrix<double> dmat = mat.convert<double>();
    Matrix<double> Q, R;
    int count = 20;
    while (count-- > 0) {
        dmat.QR_decomp(Q, R);
        if ((dmat - R * Q).diag().norm() < Matrix<double>::epsilon(dmat))
            return dmat.diag();

        dmat = R * Q;
    }
    return dmat;
}

template<class T>
Matrix<double> Matrix<T>::eigenvectors(Matrix<T> mat, Matrix<double>& eig_values) {
    if (mat.getDimension().ncols != mat.getDimension().nrows) {
        // blablabla
        return Matrix<double>(0, 0);
    }

    unsigned int size = mat.getDimension().nrows;
    std::vector< Matrix<double> > _eig_vectors;
    std::vector<double> _eig_values;
    Matrix<double> dmat = mat.convert<double>();
    Matrix<double> Imat = Matrix<double>::identity(size);

    for (int i = 0; i < eig_values.getDimension().nrows; i++) {
        double ev = eig_values[i][0];

        bool has_processed = false;
        for (int j = 0; j < _eig_values.size(); j++) {
            if (std::abs(_eig_values[j] - ev) < Matrix<double>::epsilon(dmat)) {
                has_processed = true;
                break;
            }
        }
        if (has_processed)
            continue;

        Matrix<double> A, B;
        (dmat - Imat * ev).transpose().gaussian_row_elimination(A, B, true);

        int idx = size - 1;
        do {
            Matrix<double> eig_vector = A.sub_mat(idx, idx+1, 0, size).transpose();
            _eig_vectors.push_back(eig_vector / eig_vector.norm());
            _eig_values.push_back(ev);
            idx--;
        } while (B.sub_mat(idx, idx+1, 0, size).norm() < Matrix<double>::epsilon(A) && idx >= 0);
    }

    eig_values = Matrix<double>(_eig_values.size(), 1);
    for (int i = 0; i < _eig_values.size(); i++)
        eig_values[i][0] = _eig_values[i];

    Matrix<double> vmat(size, _eig_vectors.size());
    for (int i = 0; i < _eig_vectors.size(); i++) {
        Matrix<double> v = _eig_vectors[i];
        for (int j = 0; j < size; j++) {
            vmat[j][i] = v[j][0];
        }
    }

    return vmat;
}

template<class T>
void Matrix<T>::svd(Matrix<double>& U, Matrix<double>& S, Matrix<double>& V) {
    Matrix<double> M;
    Matrix<double> eig_values;

    Matrix<double> this_mat = this->convert<double>();
    M = this_mat * this_mat.transpose();
    eig_values = Matrix<double>::eigenvalues(M);
    std::vector<double> nz_values;
    for (int i = 0; i < eig_values.getDimension().nrows; i++) {
        if (std::abs(eig_values[i][0]) < Matrix<double>::epsilon(eig_values))
            continue;

        nz_values.push_back(eig_values[i][0]);
    }
    S = Matrix<double>(nz_values.size(), nz_values.size());
    for (int i = 0; i < nz_values.size(); i++)
        S[i][i] = nz_values[i];

    Matrix<double> s_values = S.diag();
    for (int i = 0; i < nz_values.size(); i++)
        S[i][i] = std::sqrt(nz_values[i]);

    U = Matrix<double>::eigenvectors(M, s_values);

    M = this_mat.transpose() * this_mat;
    V = Matrix<double>::eigenvectors(M, s_values);

    for (int i = 0; i < V.getDimension().ncols; i++) {
        Matrix<double> u = U.sub_mat(0, U.getDimension().nrows, i, i+1);
        Matrix<double> v = V.sub_mat(0, V.getDimension().nrows, i, i+1);
        double s = S[i][i];

        if ((this_mat * v - u * s).norm() < (this_mat * v).norm())
            continue;

        for (int j = 0; j < V.getDimension().nrows; j++)
            V[j][i] *= -1;
    }
}

#endif
