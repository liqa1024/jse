package example.io

import jse.math.table.Tables

import static jse.code.UT.IO.*

double[][] data0 = [
    [1.1111, 2.2222, 3.3333],
    [11.111, 22.222, 33.333],
    [111.11, 222.22, 333.33],
    [1111.1, 2222.2, 3333.3]
]

data2csv(data0, '.temp/example/io/data.csv')
println('csv text:')
println(readAllText('.temp/example/io/data.csv'))

def data1 = csv2data('.temp/example/io/data.csv')
println('csv data:')
println(data1)


def table0 = Tables.fromRows([
    [1.1111, 2.2222, 3.3333],
    [11.111, 22.222, 33.333],
    [111.11, 222.22, 333.33],
    [1111.1, 2222.2, 3333.3]
], 'AAA', 'BBB', 'CCC')

table2csv(table0, '.temp/example/io/table.csv')
println('csv text:')
println(readAllText('.temp/example/io/table.csv'))
def table1 = csv2table('.temp/example/io/table.csv')
println('csv table:')
println('AAA: ' + table1['AAA'])
println('BBB: ' + table1['BBB'])
println('CCC: ' + table1['CCC'])


// data from https://www.kaggle.com/datasets/kamilpytlak/personal-key-indicators-of-heart-disease
def str0 = [
    ['HeartDisease', 'BMI', 'Smoking', 'AlcoholDrinking', 'Stroke', 'PhysicalHealth', 'MentalHealth', 'DiffWalking',    'Sex', 'AgeCategory',  'Race',                'Diabetic', 'PhysicalActivity', 'GenHealth', 'SleepTime', 'Asthma', 'KidneyDisease','SkinCancer'],
    [          'No', 24.21,      'No',              'No',     'No',              0.0,            0.0,          'No', 'Female',       '75-79', 'White',                      'No',               'No',      'Good',         6.0,     'No',            'No',       'Yes'],
    [          'No', 23.71,      'No',              'No',     'No',             28.0,            0.0,         'Yes', 'Female',       '40-44', 'White',                      'No',              'Yes', 'Very good',         8.0,     'No',            'No',        'No'],
    [         'Yes', 28.87,     'Yes',              'No',     'No',              6.0,            0.0,         'Yes', 'Female',       '75-79', 'Black',                      'No',               'No',      'Fair',        12.0,     'No',            'No',        'No'],
    [          'No', 21.63,      'No',              'No',     'No',             15.0,            0.0,          'No', 'Female',       '70-74', 'White',                      'No',              'Yes',      'Fair',         4.0,    'Yes',            'No',       'Yes'],
    [          'No', 31.64,     'Yes',              'No',     'No',              5.0,            0.0,         'Yes', 'Female', '80 or older', 'White',                     'Yes',               'No',      'Good',         9.0,    'Yes',            'No',        'No'],
    [          'No', 26.45,      'No',              'No',     'No',              0.0,            0.0,          'No', 'Female', '80 or older', 'White', 'No, borderline diabetes',               'No',      'Fair',         5.0,     'No',           'Yes',        'No'],
    [          'No', 40.69,      'No',              'No',     'No',              0.0,            0.0,         'Yes',   'Male',       '65-69', 'White',                      'No',              'Yes',      'Good',        10.0,     'No',            'No',        'No'],
    [         'Yes',  34.3,     'Yes',              'No',     'No',             30.0,            0.0,         'Yes',   'Male',       '60-64', 'White',                     'Yes',               'No',      'Poor',        15.0,    'Yes',            'No',        'No'],
    [          'No', 28.71,     'Yes',              'No',     'No',              0.0,            0.0,          'No', 'Female',       '55-59', 'White',                      'No',              'Yes', 'Very good',         5.0,     'No',            'No',        'No'],
    [          'No', 28.37,     'Yes',              'No',     'No',              0.0,            0.0,         'Yes',   'Male',       '75-79', 'White',                     'Yes',              'Yes', 'Very good',         8.0,     'No',            'No',        'No']
]

str2csv(str0, '.temp/example/io/str.csv')
println('csv text:')
println(readAllText('.temp/example/io/str.csv'))
def str1 = csv2str('.temp/example/io/str.csv')
println('csv str:')
println(str1)


//OUTPUT:
// csv text:
// 1.1111,2.2222,3.3333
// 11.111,22.222,33.333
// 111.11,222.22,333.33
// 1111.1,2222.2,3333.3
//
// csv data:
// 4 x 3 Matrix:
//    1.111   2.222   3.333
//    11.11   22.22   33.33
//    111.1   222.2   333.3
//    1111   2222   3333
// csv text:
// AAA,BBB,CCC
// 1.1111,2.2222,3.3333
// 11.111,22.222,33.333
// 111.11,222.22,333.33
// 1111.1,2222.2,3333.3
//
// csv table:
// AAA: 4-length Vector:
//    1.111   11.11   111.1   1111
// BBB: 4-length Vector:
//    2.222   22.22   222.2   2222
// CCC: 4-length Vector:
//    3.333   33.33   333.3   3333
// csv text:
// HeartDisease,BMI,Smoking,AlcoholDrinking,Stroke,PhysicalHealth,MentalHealth,DiffWalking,Sex,AgeCategory,Race,Diabetic,PhysicalActivity,GenHealth,SleepTime,Asthma,KidneyDisease,SkinCancer
// No,24.21,No,No,No,0.0,0.0,No,Female,75-79,White,No,No,Good,6.0,No,No,Yes
// No,23.71,No,No,No,28.0,0.0,Yes,Female,40-44,White,No,Yes,Very good,8.0,No,No,No
// Yes,28.87,Yes,No,No,6.0,0.0,Yes,Female,75-79,Black,No,No,Fair,12.0,No,No,No
// No,21.63,No,No,No,15.0,0.0,No,Female,70-74,White,No,Yes,Fair,4.0,Yes,No,Yes
// No,31.64,Yes,No,No,5.0,0.0,Yes,Female,80 or older,White,Yes,No,Good,9.0,Yes,No,No
// No,26.45,No,No,No,0.0,0.0,No,Female,80 or older,White,"No, borderline diabetes",No,Fair,5.0,No,Yes,No
// No,40.69,No,No,No,0.0,0.0,Yes,Male,65-69,White,No,Yes,Good,10.0,No,No,No
// Yes,34.3,Yes,No,No,30.0,0.0,Yes,Male,60-64,White,Yes,No,Poor,15.0,Yes,No,No
// No,28.71,Yes,No,No,0.0,0.0,No,Female,55-59,White,No,Yes,Very good,5.0,No,No,No
// No,28.37,Yes,No,No,0.0,0.0,Yes,Male,75-79,White,Yes,Yes,Very good,8.0,No,No,No
//
// csv str:
// [[HeartDisease, BMI, Smoking, AlcoholDrinking, Stroke, PhysicalHealth, MentalHealth, DiffWalking, Sex, AgeCategory, Race, Diabetic, PhysicalActivity, GenHealth, SleepTime, Asthma, KidneyDisease, SkinCancer], [No, 24.21, No, No, No, 0.0, 0.0, No, Female, 75-79, White, No, No, Good, 6.0, No, No, Yes], [No, 23.71, No, No, No, 28.0, 0.0, Yes, Female, 40-44, White, No, Yes, Very good, 8.0, No, No, No], [Yes, 28.87, Yes, No, No, 6.0, 0.0, Yes, Female, 75-79, Black, No, No, Fair, 12.0, No, No, No], [No, 21.63, No, No, No, 15.0, 0.0, No, Female, 70-74, White, No, Yes, Fair, 4.0, Yes, No, Yes], [No, 31.64, Yes, No, No, 5.0, 0.0, Yes, Female, 80 or older, White, Yes, No, Good, 9.0, Yes, No, No], [No, 26.45, No, No, No, 0.0, 0.0, No, Female, 80 or older, White, No, borderline diabetes, No, Fair, 5.0, No, Yes, No], [No, 40.69, No, No, No, 0.0, 0.0, Yes, Male, 65-69, White, No, Yes, Good, 10.0, No, No, No], [Yes, 34.3, Yes, No, No, 30.0, 0.0, Yes, Male, 60-64, White, Yes, No, Poor, 15.0, Yes, No, No], [No, 28.71, Yes, No, No, 0.0, 0.0, No, Female, 55-59, White, No, Yes, Very good, 5.0, No, No, No], [No, 28.37, Yes, No, No, 0.0, 0.0, Yes, Male, 75-79, White, Yes, Yes, Very good, 8.0, No, No, No]]

